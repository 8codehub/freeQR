package qrstudio

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import kotlin.test.*
import qrstudio.model.*
import qrstudio.platform.*
import qrstudio.vm.EditorViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class EditorRestorationTest {
    @Test fun delayedDraftWithDefaultPayloadStillGeneratesItsQr() = runTest {
        verifyRestoration(EditorState(theme=EditorTheme.Dark))
    }
    @Test fun delayedDraftGeneratesRestoredPayloadInsteadOfInitialPayload() = runTest {
        verifyRestoration(EditorState(type=ContentType.Wifi,ssid="Guest",wifiPass="secret"))
    }
    private suspend fun TestScope.verifyRestoration(draft: EditorState) {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val store=ViewModelStore()
        try {
            val read=CompletableDeferred<String?>()
            val services=PlatformServices(
                picker=object: ImagePicker {
                    override val recoveredImages=emptyFlow<PickedImage>()
                    override suspend fun pick(): PickedImage?=null
                    override suspend fun restore(reference: String): androidx.compose.ui.graphics.ImageBitmap?=null
                },
                saver=object: ImageSaver { override suspend fun save(png: ByteArray,fileName: String) {} },
                sharer=object: ImageSharer { override suspend fun share(png: ByteArray) {} },
                store=object: SessionStore {
                    override suspend fun read()=read.await()
                    override suspend fun write(value: String) {}
                },
                haptics=object: Haptics { override fun snap() {} }
            )
            val vm=EditorViewModel(services)
            store.put("editor",vm)
            assertNull(vm.state.value.matrix)
            read.complete(Json.encodeToString(draft))
            val restored=withContext(Dispatchers.Default) {
                withTimeout(5000) { vm.state.first { it.matrix!=null } }
            }
            assertEquals(draft.data,restored.data)
            assertEquals(draft.type,restored.type)
            assertEquals(draft.ssid,restored.ssid)
            assertEquals(draft.theme,restored.theme)
            assertFalse(restored.encodingFailed)
        } finally { store.clear();Dispatchers.resetMain() }
    }
}
