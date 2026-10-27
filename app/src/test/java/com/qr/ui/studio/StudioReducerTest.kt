package com.qr.ui.studio

import com.qr.domain.*
import org.junit.Assert.*
import org.junit.Test

class StudioReducerTest {
    @Test fun changingContentInvalidatesOldQrImmediately() {
        val state = StudioState(grid = QrGrid(1, booleanArrayOf(true)), rendering = false)
        val result = StudioReducer.reduce(state, StudioIntent.Field("url", "https://example.com"))
        assertNull(result.grid)
        assertTrue(result.rendering)
        assertEquals("https://example.com", result.draft.fields["url"])
    }
    @Test fun navigatingDoesNotChangeDraftOrInvalidateQr() {
        val state = StudioState(grid = QrGrid(1, booleanArrayOf(true)), rendering = false)
        val result = StudioReducer.reduce(state, StudioIntent.Navigate(StudioPage.LIBRARY))
        assertSame(state.grid, result.grid)
        assertEquals(state.draft, result.draft)
        assertFalse(result.rendering)
    }
    @Test fun newQrDetachesTheSavedItemAndClearsSensitiveFields() {
        val state = StudioState(editingId = "existing", draft = QrDraft(type = ContentType.WIFI, fields = mapOf("password" to "secret")))
        val result = StudioReducer.reduce(state, StudioIntent.New)
        assertNull(result.editingId)
        assertTrue(result.draft.fields.isEmpty())
    }
    @Test fun openingSavedQrRestoresAllEditorSettings() {
        val draft = QrDraft(title = "Guest", type = ContentType.WIFI, palette = 2, shape = ModuleShape.DOTS, correction = Correction.H, size = 2048)
        val result = StudioReducer.reduce(StudioState(page = StudioPage.LIBRARY), StudioIntent.Open(SavedQr("id", draft, 1)))
        assertEquals(draft, result.draft)
        assertEquals("id", result.editingId)
        assertEquals(StudioPage.CREATE, result.page)
    }
}
