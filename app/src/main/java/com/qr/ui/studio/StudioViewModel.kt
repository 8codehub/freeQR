package com.qr.ui.studio

import com.free.qr.R
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qr.data.*
import com.qr.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class StudioViewModel @Inject constructor(
    private val repository: StudioRepository, private val engine: QrEngine,
    private val composer: PosterRenderer, private val savedState: SavedStateHandle
) : ViewModel() {
    private val restored = savedState.get<String>("draft")?.let { runCatching { DraftCodec.decode(JSONObject(it)) }.getOrNull() } ?: QrDraft()
    private val mutableState = MutableStateFlow(StudioState(draft = restored, editingId = savedState["editingId"], appearance = repository.appearance()))
    val state = mutableState.asStateFlow()
    private val events = Channel<StudioEffect>(Channel.BUFFERED)
    val effects = events.receiveAsFlow()
    var pendingExport: ExportArtifact? = null
    private var renderJob: Job? = null
    private val undo = ArrayDeque<QrDraft>()
    private val redo = ArrayDeque<QrDraft>()
    private var lastPosterEdit = 0L
    init {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { repository.load() } }
                .onSuccess { items -> mutableState.update { it.copy(library = items, libraryReady = true) } }
                .onFailure { message(R.string.error_library_read) }
        }
        render()
    }
    fun dispatch(intent: StudioIntent) {
        if (state.value.working) return
        when (intent) {
            StudioIntent.ChoosePhoto -> viewModelScope.launch { events.send(StudioEffect.PickPhoto) }
            StudioIntent.Undo -> if (undo.isNotEmpty()) {
                redo.addLast(state.value.draft)
                restore(undo.removeLast())
            }
            StudioIntent.Redo -> if (redo.isNotEmpty()) {
                undo.addLast(state.value.draft)
                restore(redo.removeLast())
            }
            is StudioIntent.SetAppearance -> {
                mutableState.update { it.copy(working = true) }
                viewModelScope.launch {
                    try {
                        withContext(Dispatchers.IO) { repository.setAppearance(intent.value) }
                        mutableState.update { StudioReducer.reduce(it, intent) }
                    } catch (e: Exception) { message(R.string.error_appearance) }
                    finally { mutableState.update { it.copy(working = false) } }
                }
            }
            StudioIntent.Save -> save()
            is StudioIntent.Delete -> delete(intent.id)
            is StudioIntent.Export -> export(intent)
            else -> {
                val before = state.value.draft
                mutableState.update { StudioReducer.reduce(it, intent) }
                if (intent == StudioIntent.New || intent is StudioIntent.Open) { undo.clear(); redo.clear() }
                else if (before != state.value.draft) {
                    val designEdit = intent is StudioIntent.Poster || intent == StudioIntent.ResetDesign ||
                        intent is StudioIntent.Color || intent is StudioIntent.Ink || intent is StudioIntent.Paper ||
                        intent is StudioIntent.Shape || intent is StudioIntent.Reliability || intent is StudioIntent.Size
                    if (designEdit) {
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (intent !is StudioIntent.Poster || now - lastPosterEdit > 400 || undo.isEmpty()) {
                            undo.addLast(before); if (undo.size > 30) undo.removeFirst()
                        }
                        if (intent is StudioIntent.Poster) lastPosterEdit = now
                    } else {
                        // A design undo must never bring back a previous QR payload.
                        undo.clear(); lastPosterEdit = 0
                    }
                    redo.clear()
                }
                mutableState.update { it.copy(canUndo = undo.isNotEmpty(), canRedo = redo.isNotEmpty()) }
                savedState["draft"] = DraftCodec.encode(state.value.draft).toString()
                savedState["editingId"] = state.value.editingId
                if (state.value.draft != before) render(if (intent is StudioIntent.Poster) 32 else 160)
            }
        }
    }
    private fun restore(draft: QrDraft) {
        mutableState.update { StudioReducer.reduce(it, StudioIntent.RestoreDraft(draft)).copy(canUndo = undo.isNotEmpty(), canRedo = redo.isNotEmpty()) }
        savedState["draft"] = DraftCodec.encode(draft).toString()
        lastPosterEdit = 0
        render()
    }
    private fun render(wait: Long = 160) {
        renderJob?.cancel()
        val draft = state.value.draft
        renderJob = viewModelScope.launch {
            delay(wait)
            try {
                val rendered = withContext(Dispatchers.Default) {
                    val grid = engine.generate(draft)
                    val qr = engine.bitmap(grid, draft, 640)
                    val poster = if (draft.poster.enabled) runCatching { composer.render(grid, draft, maxOf(800, ((grid.width + 8) / (draft.poster.ratio.aspect * draft.poster.qrScale * .87f)).toInt() + 4).coerceAtMost(2048)) } else null
                    Rendered(grid, qr, poster?.getOrNull(), poster?.exceptionOrNull()?.let {
                        if (it is PosterProblem) it.text else LocalizedText(R.string.error_photo)
                    })
                }
                mutableState.update { it.copy(grid = rendered.grid, preview = rendered.qr, posterPreview = rendered.poster, posterError = rendered.problem, rendering = false, error = null) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val backdrop = if (draft.poster.enabled) withContext(Dispatchers.Default) { runCatching { composer.render(null, draft, 800) } } else null
                mutableState.update { it.copy(preview = null, grid = null, posterPreview = backdrop?.getOrNull(),
                    posterError = backdrop?.exceptionOrNull()?.let { problem -> if (problem is PosterProblem) problem.text else LocalizedText(R.string.error_photo) }, rendering = false,
                    error = if (e is InvalidContent) e.text else LocalizedText(R.string.error_capacity)) }
            }
        }
    }
    private fun save() {
        val current = state.value
        if (!current.libraryReady || current.grid == null || current.rendering) return
        if (current.library.size >= 100 && current.editingId == null) { message(R.string.error_library_full); return }
        val item = SavedQr(current.editingId ?: UUID.randomUUID().toString(), current.draft, System.currentTimeMillis())
        persist(listOf(item) + current.library.filterNot { it.id == item.id }) {
            mutableState.update { it.copy(editingId = item.id) }; savedState["editingId"] = item.id
            message(R.string.saved_message)
        }
    }
    private fun delete(id: String) {
        if (!state.value.libraryReady) return
        persist(state.value.library.filterNot { it.id == id }) {
            if (state.value.editingId == id) { mutableState.update { it.copy(editingId = null) }; savedState["editingId"] = null }
            message(R.string.deleted_message)
        }
    }
    private fun persist(items: List<SavedQr>, success: () -> Unit) {
        mutableState.update { it.copy(working = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.store(items) }
                mutableState.update { it.copy(library = items) }; success()
            } catch (e: Exception) { message(R.string.error_save_changes) }
            finally { mutableState.update { it.copy(working = false) } }
        }
    }
    private fun export(intent: StudioIntent.Export) {
        val current = state.value
        val grid = current.grid ?: return
        if (current.rendering) return
        mutableState.update { it.copy(working = true) }
        viewModelScope.launch {
            try {
                val artifact = withContext(Dispatchers.Default) {
                    if (intent.composition) composer.export(grid, current.draft) else engine.export(grid, current.draft, intent.format)
                }
                pendingExport = artifact
                events.send(StudioEffect.Export(artifact, intent.share, intent.gallery))
            } catch (e: PosterProblem) { viewModelScope.launch { events.send(StudioEffect.Message(e.text)) } }
            catch (e: Exception) { message(R.string.error_export) }
            finally { mutableState.update { it.copy(working = false) } }
        }
    }
    private data class Rendered(val grid: QrGrid, val qr: android.graphics.Bitmap, val poster: android.graphics.Bitmap?, val problem: LocalizedText?)
    fun message(@StringRes id: Int, vararg args: Any) { viewModelScope.launch { events.send(StudioEffect.Message(LocalizedText(id, args.toList()))) } }
}
