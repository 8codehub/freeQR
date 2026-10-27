package com.qr.domain

import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.Decoder
import org.junit.Assert.*
import org.junit.Test
import javax.xml.parsers.DocumentBuilderFactory

class QrEngineTest {
    private val engine = QrEngine()
    @Test fun everyContentTypeDecodesToItsExactPayloadAtEveryRecoveryLevel() {
        val fields = mapOf("url" to "https://example.com/hello", "text" to "Hello café 世界", "ssid" to "Cafe;Guest", "password" to "complex:pass", "email" to "hi@example.com", "subject" to "Hi & hello", "body" to "How are you?", "phone" to "+1 555 010 2020", "name" to "Alex Morgan")
        ContentType.entries.forEach { type -> Correction.entries.forEach { level ->
            val draft = QrDraft(type = type, fields = fields, correction = level)
            val grid = engine.generate(draft)
            val matrix = BitMatrix(grid.width)
            for (y in 0 until grid.width) for (x in 0 until grid.width) if (grid[x,y]) matrix.set(x,y)
            assertEquals("$type / $level", QrPayload.encode(draft), Decoder().decode(matrix).text)
        } }
    }
    @Test fun svgHasQuietZoneAndSafeMarkupForEveryShapeAndPalette() {
        ModuleShape.entries.forEach { shape -> palettes.indices.forEach { palette ->
            val draft = QrDraft(type = ContentType.TEXT, fields = mapOf("text" to "<script>& test"), shape = shape, palette = palette)
            val grid = engine.generate(draft)
            val svg = engine.svg(grid, draft)
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(svg.byteInputStream())
            assertEquals("0 0 ${grid.width+8} ${grid.width+8}", doc.documentElement.getAttribute("viewBox"))
            assertFalse(svg.contains("script"))
            assertTrue(svg.contains("x=\"4\" y=\"4\" width=\"1\" height=\"1\""))
        } }
    }
}
