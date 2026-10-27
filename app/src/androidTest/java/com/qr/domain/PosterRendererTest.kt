package com.qr.domain

import android.graphics.*
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.free.qr.R
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.qr.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PosterRendererTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val engine = QrEngine()
    private val renderer = PosterRenderer(context, engine)
    @Test fun layeredPhotoDesignsRemainScannableAcrossFramesFormatsAndDirections() {
        val source = Bitmap.createBitmap(900, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(source)
        val paint = Paint().apply { shader = LinearGradient(0f,0f,900f,600f, Color.rgb(245,171,109),Color.rgb(39,73,92),Shader.TileMode.CLAMP) }
        canvas.drawRect(0f,0f,900f,600f,paint)
        val file = File(File(context.cacheDir,"exports").apply { mkdirs() }, "composer-verification.png")
        file.outputStream().use { source.compress(Bitmap.CompressFormat.PNG,100,it) }; source.recycle()
        val uri = FileProvider.getUriForFile(context,"${context.packageName}.files",file)
        try {
            CanvasRatio.entries.forEach { ratio -> QrFrame.entries.forEach { frame -> listOf(false,true).forEach { reversed ->
                val draft = QrDraft(fields = mapOf("url" to "https://example.com/menu"), shape = ModuleShape.ROUNDED,
                    poster = PosterDesign(enabled = true, photoUri = uri.toString(), ratio = ratio, frame = frame,
                        reversed = reversed, qrScale = .62f, x = if (reversed) .8f else .2f, y = if (reversed) 1f else 0f, caption = "Scan me · القائمة"))
                val bitmap = renderer.render(engine.generate(draft), draft, 1024, validateSize = true)
                try {
                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
                    val code = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width,bitmap.height,pixels))), mapOf(DecodeHintType.TRY_HARDER to true))
                    assertEquals("$ratio/$frame/$reversed", QrPayload.encode(draft), code.text)
                } finally { bitmap.recycle() }
            } } }
        } finally { file.delete() }
    }
    @Test fun savingRestoresEveryLayerAndOldLibraryEntriesStillOpen() {
        val draft = QrDraft(title = "Menu", fields = mapOf("url" to "https://example.com"),
            poster = PosterDesign(enabled = true, photoUri = "content://photos/local/1", ratio = CanvasRatio.STORY,
                opacity = .4f, reversed = true, photoZoom = 1.7f, qrScale = .6f, x = .2f, y = .9f, frame = QrFrame.DOUBLE, caption = "قائمة الطعام"))
        assertEquals(draft, DraftCodec.decode(DraftCodec.encode(draft)))
        val legacy = DraftCodec.encode(draft).apply { remove("poster") }
        assertEquals(PosterDesign(), DraftCodec.decode(legacy).poster)
    }
    @Test fun denseCodesBlockInsufficientExportResolution() {
        val draft = QrDraft(type = ContentType.TEXT, fields = mapOf("text" to "Some detailed text. ".repeat(50)),
            correction = Correction.H, size = 512, poster = PosterDesign(enabled = true, ratio = CanvasRatio.STORY, qrScale = .26f))
        val error = assertThrows(PosterProblem::class.java) { renderer.export(engine.generate(draft),draft) }
        assertEquals(R.string.error_poster_size,error.text.id)
    }
    @Test fun brokenPhotoAccessShowsRecoverableError() {
        val draft = QrDraft(fields = mapOf("url" to "https://example.com"), poster = PosterDesign(enabled = true, photoUri = "content://missing-photo/image"))
        val error = assertThrows(PosterProblem::class.java) { renderer.render(engine.generate(draft),draft,800) }
        assertEquals(R.string.error_photo,error.text.id)
    }
    @Test fun backgroundAndGradientCanBeEditedOnTheMainPreviewBeforeAddingQrText() {
        val draft = QrDraft(poster = PosterDesign(enabled = true,ratio = CanvasRatio.SQUARE,
            topColor = 0xFF000000,bottomColor = 0xFFFFFFFF,opacity = 1f))
        val forward = renderer.render(null,draft,512)
        val reverse = renderer.render(null,draft.copy(poster=draft.poster.copy(reversed=true)),512)
        try {
            assertTrue(Color.red(forward.getPixel(256,5)) < Color.red(forward.getPixel(256,506)))
            assertTrue(Color.red(reverse.getPixel(256,5)) > Color.red(reverse.getPixel(256,506)))
        } finally { forward.recycle();reverse.recycle() }
    }

}
