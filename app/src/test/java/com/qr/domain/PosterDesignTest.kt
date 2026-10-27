package com.qr.domain

import org.junit.Assert.*
import org.junit.Test
import com.qr.ui.studio.*

class PosterDesignTest {
    @Test fun invalidImportedLayerValuesAreNormalized() {
        val design = PosterDesign(photoUri = "https://remote/image.png", opacity = Float.NaN, qrScale = 8f, x = -5f, y = Float.POSITIVE_INFINITY, caption = "a".repeat(200)).normalized()
        assertNull(design.photoUri)
        assertEquals(.65f, design.opacity)
        assertEquals(.76f, design.qrScale)
        assertEquals(0f, design.x)
        assertEquals(.65f,design.y)
        assertEquals(80,design.caption.length)
    }
    @Test fun editingPhotoLayersPreservesQrPayloadAndMatrix() {
        val grid = QrGrid(1,booleanArrayOf(true))
        val state = StudioState(draft = QrDraft(fields = mapOf("url" to "https://example.com")), grid = grid, rendering = false)
        val next = StudioReducer.reduce(state, StudioIntent.Poster(PosterDesign(topColor = 0xFF000000, x = .2f)))
        assertSame(grid,next.grid)
        assertEquals(state.draft.fields,next.draft.fields)
        assertTrue(next.rendering)
        assertTrue(next.draft.poster.enabled)
    }
    @Test fun savedArtworkLoadsIntoTheSameGeneratorScreen() {
        val draft = QrDraft(poster = PosterDesign(enabled = true))
        val next = StudioReducer.reduce(StudioState(),StudioIntent.Open(SavedQr("design",draft,1)))
        assertEquals(StudioPage.CREATE,next.page)
        assertTrue(next.draft.poster.enabled)
    }
    @Test fun resettingDesignPreservesTheQrContentAndReturnsToPlainPreview() {
        val draft = QrDraft(fields=mapOf("text" to "Hello"),poster=PosterDesign(enabled=true,photoUri="content://local/image"))
        val next = StudioReducer.reduce(StudioState(draft=draft),StudioIntent.ResetDesign)
        assertFalse(next.draft.poster.enabled)
        assertEquals(draft.fields,next.draft.fields)
        assertNull(next.draft.poster.photoUri)
    }

}
