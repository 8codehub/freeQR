package qrstudio.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import qrstudio.model.*
import qrstudio.platform.*
import qrstudio.qr.*
import qrstudio.render.snapCenter

@OptIn(FlowPreview::class)
class EditorViewModel(private val services: PlatformServices,initialJson: String?=null): ViewModel() {
    private val json=Json { ignoreUnknownKeys=true;encodeDefaults=true }
    private fun decode(value: String?)=value?.let { runCatching { json.decodeFromString<EditorState>(it).sanitized() }.getOrNull() }
    private val mutable=MutableStateFlow(decode(initialJson) ?: EditorState())
    val state=mutable.asStateFlow()
    private val encoder=QrEncoder()
    private var edited=false
    private var snackJob: Job?=null
    init {
        val draftLoaded=CompletableDeferred<Unit>()
        viewModelScope.launch { services.picker.recoveredImages.collect { image -> edit { copy(bgMode=BackgroundMode.Image,bgImage=image.bitmap,bgImageReference=image.reference) } } }
        viewModelScope.launch {
            if(initialJson==null) {
                val stored=runCatching { services.store.read() }.getOrNull()
                if(!edited) decode(stored)?.let { mutable.value=it }
            }
            // Start encoding only after draft restoration; an identical restored payload
            // otherwise clears the transient matrix without retriggering distinctUntilChanged.
            draftLoaded.complete(Unit)
            mutable.value.bgImageReference?.let { reference ->
                val image=runCatching { services.picker.restore(reference) }.getOrNull()
                mutable.update { if(it.bgImageReference==reference) it.copy(bgImage=image) else it }
            }
            state.map { json.encodeToString(it) }.distinctUntilChanged().debounce(180).collectLatest { runCatching { services.store.write(it) } }
        }
        viewModelScope.launch {
            draftLoaded.await()
            state.map { buildPayload(it) to it.ecl }.distinctUntilChanged().collectLatest { (payload,ecl) ->
                val matrix=withContext(Dispatchers.Default) { encoder.encode(payload,ecl) }
                mutable.update { if(buildPayload(it)==payload && it.ecl==ecl) it.copy(matrix=matrix,encodingFailed=matrix==null) else it }
            }
        }
    }
    fun snapshot()=json.encodeToString(state.value)
    fun edit(change: EditorState.()->EditorState) { edited=true;mutable.update { previous -> val next=previous.change();if(buildPayload(previous)!=buildPayload(next) || previous.ecl!=next.ecl) next.copy(matrix=null,encodingFailed=false) else next } }
    fun tab(tab: EditorTab)=edit { copy(tab=tab,selection=if(tab==EditorTab.QrStyle) Selection.Qr else selection) }
    fun select(selection: Selection)=edit { copy(selection=selection,tab=if(selection is Selection.Text) EditorTab.Text else tab) }
    fun updateText(change: TextLayer.()->TextLayer) {
        val selected=state.value.selection as? Selection.Text ?: return
        edit { copy(texts=texts.map { if(it.id==selected.id) it.change() else it }) }
    }
    fun addText()=edit { withAddedText(currentTimeMillis()) }
    fun deleteText()=edit { withDeletedText() }
    fun drag(selection: Selection,x: Float,y: Float) {
        val snapped=snapCenter(x,y,state.value.snapToCenter)
        val newlySnapped=(snapped.vertical && !state.value.guideX) || (snapped.horizontal && !state.value.guideY)
        if(newlySnapped) services.haptics.snap()
        edit { when(selection) {
            Selection.Qr -> copy(qx=snapped.x,qy=snapped.y,guideX=snapped.vertical,guideY=snapped.horizontal)
            is Selection.Text -> copy(texts=texts.map { if(it.id==selection.id) it.copy(x=snapped.x,y=snapped.y) else it },guideX=snapped.vertical,guideY=snapped.horizontal)
            Selection.None -> this
        } }
    }
    fun endDrag()=edit { copy(guideX=false,guideY=false) }
    fun chooseImage() { viewModelScope.launch {
        runCatching { services.picker.pick() }.onSuccess { image -> if(image!=null) edit { copy(bgMode=BackgroundMode.Image,bgImage=image.bitmap,bgImageReference=image.reference) } }
            .onFailure { snack("The image could not be opened. Please try another image.") }
    } }
    fun removeImage()=edit { copy(bgImage=null,bgImageReference=null) }
    fun snack(message: String) {
        snackJob?.cancel();mutable.update { it.copy(snack=message) }
        snackJob=viewModelScope.launch { delay(3000);mutable.update { it.copy(snack=null) } }
    }
    fun export(share: Boolean,render: suspend (EditorState)->ByteArray) {
        val snapshot=state.value
        if(snapshot.exporting) return
        if(snapshot.matrix==null) { snack(if(share) "Nothing to share. Check the QR content." else "Nothing to save. Check the QR content.");return }
        mutable.update { it.copy(exporting=true) }
        viewModelScope.launch {
            try {
                val png=withContext(Dispatchers.Default) { render(snapshot) }
                if(share) {
                    try { services.sharer.share(png) }
                    catch(e: Exception) { services.saver.save(png,exportFileName());snack("Sharing failed. Image saved instead.") }
                } else { services.saver.save(png,exportFileName());snack("Saved to Pictures/QR Studio") }
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { snack("Could not export the image. Please try again.") }
            finally { mutable.update { it.copy(exporting=false) } }
        }
    }
}
