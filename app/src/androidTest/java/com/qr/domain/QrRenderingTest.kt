package com.qr.domain

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QrRenderingTest {
    @Test fun exportedPngStylesDecodeAtEveryResolution() {
        val engine = QrEngine()
        ModuleShape.entries.forEach { shape -> palettes.indices.forEach { palette -> listOf(512,1024,2048).forEach { size ->
            val draft = QrDraft(fields = mapOf("url" to "https://example.com/hello?message=caf%C3%A9"), shape = shape, palette = palette, size = size)
            val bitmap = engine.bitmap(engine.generate(draft), draft)
            try {
                val pixels = IntArray(size * size)
                bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
                val decoded = try { MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(size, size, pixels)))) } catch (e: Exception) { throw AssertionError("Decode failed: $shape / $palette / $size", e) }
                assertEquals("$shape / $palette / $size", QrPayload.encode(draft), decoded.text)
                assertEquals(palettes[palette].paper.toInt(), bitmap.getPixel(0,0))
            } finally { bitmap.recycle() }
        } } }
    }
    @Test fun denseUnicodeAndWifiCodesDecodeAfterStyling() {
        val engine = QrEngine()
        val drafts = listOf(
            QrDraft(type = ContentType.TEXT, fields = mapOf("text" to "Hello café 世界 ".repeat(35)), correction = Correction.H),
            QrDraft(type = ContentType.WIFI, fields = mapOf("ssid" to "Cafe;Guest", "password" to "p@ss;word")))
        drafts.forEach { base -> ModuleShape.entries.forEach { shape ->
            val draft = base.copy(shape = shape)
            val bitmap = engine.bitmap(engine.generate(draft), draft)
            try {
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                val decoded = try { MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels)))) } catch (e: Exception) { throw AssertionError("Decode failed: ${draft.type} / $shape / ${bitmap.width}", e) }
                assertEquals(QrPayload.encode(draft), decoded.text)
            } finally { bitmap.recycle() }
        } }
    }
    @Test fun originalForegroundBackgroundColorsAndTransparencyArePreserved() {
        val engine = QrEngine()
        listOf(0xFFFFFFFFL,0xFFB2EBF2L,0xFFFFF59DL,0L).forEach { paper ->
            val draft = QrDraft(type = ContentType.TEXT, fields = mapOf("text" to "https://example.com"), customInk = 0xFF000000L, customPaper = paper)
            val bitmap = engine.bitmap(engine.generate(draft),draft)
            try {
                assertEquals(paper.toInt(),bitmap.getPixel(0,0))
                val flattened = android.graphics.Bitmap.createBitmap(bitmap.width,bitmap.height,android.graphics.Bitmap.Config.ARGB_8888)
                try {
                    val canvas = android.graphics.Canvas(flattened)
                    canvas.drawColor(android.graphics.Color.WHITE);canvas.drawBitmap(bitmap,0f,0f,null)
                    val pixels=IntArray(flattened.width*flattened.height)
                    flattened.getPixels(pixels,0,flattened.width,0,0,flattened.width,flattened.height)
                    val decoded=MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(flattened.width,flattened.height,pixels))))
                    assertEquals(QrPayload.encode(draft),decoded.text)
                } finally { flattened.recycle() }
            } finally { bitmap.recycle() }
        }
        val transparent = QrDraft(type=ContentType.TEXT,fields=mapOf("text" to "hello"),customPaper=0L)
        assertTrue(engine.svg(engine.generate(transparent),transparent).contains("fill-opacity=\"0.0\""))
    }

}
