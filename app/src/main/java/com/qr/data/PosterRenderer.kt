package com.qr.data

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.util.LruCache
import com.free.qr.R
import com.qr.domain.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

class PosterProblem(val text: LocalizedText) : IllegalArgumentException()
@Singleton
class PosterRenderer @Inject constructor(@ApplicationContext private val context: Context, private val engine: QrEngine) {
    private val photos = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    private fun photo(uri: String): Bitmap {
        photos.get(uri)?.let { return it }
        val result = try {
            val parsed = Uri.parse(uri)
            check(parsed.scheme == "content")
            if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, parsed)) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val factor = min(1f, 2048f / max(info.size.width, info.size.height))
                    decoder.setTargetSize(max(1, (info.size.width * factor).roundToInt()), max(1, (info.size.height * factor).roundToInt()))
                }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(parsed).use { BitmapFactory.decodeStream(it, null, bounds) }
                val options = BitmapFactory.Options().apply {
                    var sample = 1
                    while (max(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
                    inSampleSize = sample
                }
                val bitmap = checkNotNull(context.contentResolver.openInputStream(parsed).use { BitmapFactory.decodeStream(it, null, options) })
                val orientation = context.contentResolver.openInputStream(parsed).use { stream ->
                    if (stream == null) 1 else runCatching { android.media.ExifInterface(stream).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)
                }
                val matrix = Matrix().apply {
                    when (orientation) {
                        2 -> setScale(-1f, 1f); 3 -> setRotate(180f); 4 -> setScale(1f, -1f)
                        5 -> { setRotate(90f); postScale(-1f, 1f) }; 6 -> setRotate(90f)
                        7 -> { setRotate(270f); postScale(-1f, 1f) }; 8 -> setRotate(270f)
                    }
                }
                if (matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also { bitmap.recycle() }
            }
        } catch (e: Exception) { throw PosterProblem(LocalizedText(R.string.error_photo)) }
        photos.put(uri, result)
        return result
    }
    fun render(grid: QrGrid?, draft: QrDraft, maxEdge: Int, validateSize: Boolean = false): Bitmap {
        val design = draft.poster.normalized()
        val width = (maxEdge * design.ratio.aspect).roundToInt()
        val height = maxEdge
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(design.bottomColor.toInt())
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            design.photoUri?.let { uri ->
                val source = photo(uri)
                val scale = max(width.toFloat() / source.width, height.toFloat() / source.height) * design.photoZoom
                val sw = source.width * scale; val sh = source.height * scale
                canvas.drawBitmap(source, null, RectF((width-sw)/2, (height-sh)/2, (width+sw)/2, (height+sh)/2), paint)
            }
            if (design.gradientEnabled) {
                val alpha = (design.opacity * 255).roundToInt()
                fun shade(color: Long) = (color.toInt() and 0xFFFFFF) or (alpha shl 24)
                val top = shade(if (design.reversed) design.bottomColor else design.topColor)
                val bottom = shade(if (design.reversed) design.topColor else design.bottomColor)
                paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), top, bottom, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                paint.shader = null
            }
            if (grid != null) {
            val outer = geometry(width.toFloat(), height.toFloat(), design)
            val inset = if (design.frame == QrFrame.NONE) 0f else outer.width() * .065f
            val qrSide = (outer.width() - inset * 2).roundToInt()
            if (qrSide < grid.width + 8 || (validateSize && qrSide < (grid.width+8) * 2)) throw PosterProblem(LocalizedText(R.string.error_poster_size))
            val qr = engine.bitmap(grid, draft, qrSide)
            try {
                val captionHeight = if (design.caption.isNotBlank()) outer.width() * .19f else 0f
                val plate = RectF(outer.left, outer.top, outer.right, outer.bottom + captionHeight)
                paint.color = Color.WHITE
                canvas.drawRoundRect(plate, if (design.frame == QrFrame.CARD) outer.width() * .07f else 0f, if (design.frame == QrFrame.CARD) outer.width() * .07f else 0f, paint)
                canvas.drawBitmap(qr, (outer.left + inset).roundToInt().toFloat(), (outer.top + inset).roundToInt().toFloat(), null)
                val ink = draft.colors.ink.toInt()
                if (design.frame in listOf(QrFrame.OUTLINE, QrFrame.DOUBLE)) {
                    paint.color = ink; paint.style = Paint.Style.STROKE; paint.strokeWidth = max(1f, width * .003f)
                    val border = RectF(plate).apply { inset(paint.strokeWidth, paint.strokeWidth) }
                    canvas.drawRect(border, paint)
                    if (design.frame == QrFrame.DOUBLE) { border.inset(paint.strokeWidth*2, paint.strokeWidth*2); canvas.drawRect(border, paint) }
                    paint.style = Paint.Style.FILL
                }
                if (captionHeight > 0) {
                    paint.color = ink; paint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    paint.textAlign = Paint.Align.LEFT; paint.textSize = outer.width() * .066f
                    val textPaint = android.text.TextPaint(paint)
                    val textWidth = (outer.width() * .86f).roundToInt()
                    val layout = android.text.StaticLayout.Builder.obtain(design.caption, 0, design.caption.length, textPaint, textWidth)
                        .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER).setIncludePad(false)
                        .setTextDirection(android.text.TextDirectionHeuristics.FIRSTSTRONG_LTR)
                        .setMaxLines(1).setEllipsize(android.text.TextUtils.TruncateAt.END).build()
                    canvas.save()
                    canvas.translate(outer.centerX() - textWidth / 2, outer.bottom + (captionHeight - layout.height) / 2)
                    layout.draw(canvas)
                    canvas.restore()
                }
            } finally { qr.recycle() }
            }
            return bitmap
        } catch (e: Exception) { bitmap.recycle(); throw e }
    }
    fun export(grid: QrGrid, draft: QrDraft): ExportArtifact {
        val bitmap = render(grid, draft, draft.size, validateSize = true)
        val bytes = try { ByteArrayOutputStream().use { out -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)); out.toByteArray() } } finally { bitmap.recycle() }
        val name = draft.title.replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim().take(60).ifBlank { "Easy QR Studio" }
        return ExportArtifact(bytes, "$name.png", "image/png")
    }
    companion object {
        fun geometry(width: Float, height: Float, design: PosterDesign): RectF {
            val side = width * design.qrScale
            val margin = width * .04f
            val caption = if (design.caption.isNotBlank()) side * .19f else 0f
            val left = margin + design.x * (width - 2*margin - side)
            val top = margin + design.y * (height - 2*margin - side - caption)
            return RectF(left, top, left + side, top + side)
        }
    }
}
