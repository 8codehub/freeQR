package com.qr.ui.studio

import android.graphics.Bitmap
import com.qr.domain.*

enum class StudioPage { CREATE, LIBRARY, DESIGN, GUIDE }
enum class EditorTab { CONTENT, DESIGN, EXPORT }
data class StudioState(
    val page: StudioPage = StudioPage.CREATE, val tab: EditorTab = EditorTab.CONTENT,
    val draft: QrDraft = QrDraft(), val editingId: String? = null,
    val library: List<SavedQr> = emptyList(), val query: String = "",
    val preview: Bitmap? = null, val grid: QrGrid? = null,
    val posterPreview: Bitmap? = null, val posterError: LocalizedText? = null,
    val canUndo: Boolean = false, val canRedo: Boolean = false,
    val error: LocalizedText? = null, val rendering: Boolean = true, val working: Boolean = false,
    val appearance: Appearance = Appearance.SYSTEM, val libraryReady: Boolean = false
)
sealed interface StudioIntent {
    data class Navigate(val page: StudioPage) : StudioIntent
    data class Tab(val tab: EditorTab) : StudioIntent
    data class Type(val type: ContentType) : StudioIntent
    data class Field(val key: String, val value: String) : StudioIntent
    data class Title(val value: String) : StudioIntent
    data class Color(val index: Int) : StudioIntent
    data class Ink(val color: Long) : StudioIntent
    data class Paper(val color: Long) : StudioIntent
    data class Shape(val shape: ModuleShape) : StudioIntent
    data class Reliability(val correction: Correction) : StudioIntent
    data class Size(val size: Int) : StudioIntent
    data class Wifi(val open: Boolean, val hidden: Boolean) : StudioIntent
    data class SetAppearance(val value: Appearance) : StudioIntent
    data class Poster(val design: PosterDesign) : StudioIntent
    data class RestoreDraft(val draft: QrDraft) : StudioIntent
    data object ResetDesign : StudioIntent
    data object ChoosePhoto : StudioIntent
    data object Undo : StudioIntent
    data object Redo : StudioIntent
    data class Search(val value: String) : StudioIntent
    data class Open(val item: SavedQr) : StudioIntent
    data class Delete(val id: String) : StudioIntent
    data class Export(val format: ExportFormat, val share: Boolean = false, val composition: Boolean = false, val gallery: Boolean = false) : StudioIntent
    data object Save : StudioIntent
    data object New : StudioIntent
}
sealed interface StudioEffect {
    data object PickPhoto : StudioEffect
    data class Message(val text: LocalizedText) : StudioEffect
    data class Export(val artifact: ExportArtifact, val share: Boolean, val gallery: Boolean = false) : StudioEffect
}
object StudioReducer {
    fun reduce(state: StudioState, intent: StudioIntent): StudioState {
        val next = when (intent) {
            is StudioIntent.Navigate -> state.copy(page = intent.page)
            is StudioIntent.Tab -> state.copy(tab = intent.tab)
            is StudioIntent.Type -> state.copy(draft = state.draft.copy(type = intent.type))
            is StudioIntent.Field -> state.copy(draft = state.draft.copy(fields = state.draft.fields + (intent.key to intent.value)))
            is StudioIntent.Title -> state.copy(draft = state.draft.copy(title = intent.value.take(80)))
            is StudioIntent.Color -> state.copy(draft = state.draft.copy(palette = intent.index.coerceIn(palettes.indices), customInk = null, customPaper = null))
            is StudioIntent.Ink -> state.copy(draft = state.draft.copy(customInk = intent.color))
            is StudioIntent.Paper -> state.copy(draft = state.draft.copy(customPaper = intent.color))
            is StudioIntent.Shape -> state.copy(draft = state.draft.copy(shape = intent.shape))
            is StudioIntent.Reliability -> state.copy(draft = state.draft.copy(correction = intent.correction))
            is StudioIntent.Size -> state.copy(draft = state.draft.copy(size = if (intent.size in listOf(512,1024,2048)) intent.size else 1024))
            is StudioIntent.Wifi -> state.copy(draft = state.draft.copy(openWifi = intent.open, hiddenWifi = intent.hidden))
            is StudioIntent.SetAppearance -> state.copy(appearance = intent.value)
            StudioIntent.ResetDesign -> state.copy(draft = state.draft.copy(poster = PosterDesign()))
            is StudioIntent.Poster -> state.copy(draft = state.draft.copy(poster = intent.design.normalized().copy(enabled = true)))
            is StudioIntent.RestoreDraft -> state.copy(draft = intent.draft)
            is StudioIntent.Search -> state.copy(query = intent.value)
            is StudioIntent.Open -> state.copy(page = StudioPage.CREATE, tab = EditorTab.CONTENT, draft = intent.item.draft, editingId = intent.item.id)
            StudioIntent.New -> state.copy(page = StudioPage.CREATE, tab = EditorTab.CONTENT, draft = QrDraft(), editingId = null)
            else -> state
        }
        return if (next.draft != state.draft) {
            val qrChanged = next.draft.copy(poster = state.draft.poster) != state.draft
            next.copy(preview = if (qrChanged) null else state.preview, grid = if (qrChanged) null else state.grid,
                posterError = null, error = null, rendering = true)
        } else next
    }
}
