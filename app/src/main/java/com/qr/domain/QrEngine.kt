package com.qr.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class QrGrid(val width: Int, private val cells: BooleanArray) {
    operator fun get(x: Int, y: Int) = cells[y * width + x]
    private val alignmentCenters: IntArray = if (width >= 21 && (width - 17) % 4 == 0)
        com.google.zxing.qrcode.decoder.Version.getVersionForNumber((width - 17) / 4).alignmentPatternCenters else intArrayOf()
    fun isFunctional(x: Int, y: Int): Boolean {
        if ((x <= 8 && y <= 8) || (x >= width - 8 && y <= 8) || (x <= 8 && y >= width - 8)) return true
        if (x == 6 || y == 6) return true
        if (width >= 45 && ((x >= width - 11 && y < 6) || (y >= width - 11 && x < 6))) return true
        return alignmentCenters.any { cx -> alignmentCenters.any { cy -> kotlin.math.abs(x - cx) <= 2 && kotlin.math.abs(y - cy) <= 2 } }
    }
}
enum class ExportFormat(val extension: String, val mime: String) { PNG("png", "image/png"), SVG("svg", "image/svg+xml") }
data class ExportArtifact(val bytes: ByteArray, val name: String, val mime: String)
class QrEngine @Inject constructor() {
    fun generate(draft: QrDraft): QrGrid {
        val payload = QrPayload.encode(draft)
        val matrix = Encoder.encode(payload, ErrorCorrectionLevel.valueOf(draft.correction.name),
            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")).matrix
        return QrGrid(matrix.width, BooleanArray(matrix.width * matrix.width) { matrix[it % matrix.width, it / matrix.width].toInt() == 1 })
    }
    fun bitmap(grid: QrGrid, draft: QrDraft, size: Int = draft.size): Bitmap {
        val palette = draft.colors
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(palette.paper.toInt())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.ink.toInt() }
        val unit = (size / (grid.width + 8)).toFloat()
        val offset = ((size - grid.width * unit) / 2).toInt().toFloat()
        for (y in 0 until grid.width) for (x in 0 until grid.width) if (grid[x, y]) {
            val left = offset + x * unit; val top = offset + y * unit
            val rect = RectF(left, top, left + unit, top + unit)
            when (if (grid.isFunctional(x, y)) ModuleShape.SQUARE else draft.shape) {
                ModuleShape.SQUARE -> { paint.isAntiAlias = false; canvas.drawRect(rect, paint) }
                ModuleShape.ROUNDED -> { paint.isAntiAlias = true; canvas.drawRoundRect(rect, unit * .24f, unit * .24f, paint) }
                ModuleShape.DOTS -> { paint.isAntiAlias = true; canvas.drawCircle(left + unit / 2, top + unit / 2, unit * .48f, paint) }
            }
        }
        return bitmap
    }
    fun export(grid: QrGrid, draft: QrDraft, format: ExportFormat): ExportArtifact {
        val bytes = when (format) {
            ExportFormat.PNG -> ByteArrayOutputStream().use { out ->
                val bitmap = bitmap(grid, draft)
                try { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)); out.toByteArray() } finally { bitmap.recycle() }
            }
            ExportFormat.SVG -> svg(grid, draft).toByteArray(Charsets.UTF_8)
        }
        val name = draft.title.replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim().take(60).ifBlank { "Easy QR" }
        return ExportArtifact(bytes, "$name.${format.extension}", format.mime)
    }
    fun svg(grid: QrGrid, draft: QrDraft): String = buildString {
        fun hex(value: Long) = "#%06X".format(value and 0xFFFFFF)
        val extent = grid.width + 8
        append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"${draft.size}\" height=\"${draft.size}\" viewBox=\"0 0 $extent $extent\">")
        append("<rect width=\"$extent\" height=\"$extent\" fill=\"${hex(draft.colors.paper)}\" fill-opacity=\"${((draft.colors.paper ushr 24) and 255) / 255.0}\"/>")
        append("<g fill=\"${hex(draft.colors.ink)}\">")
        for (y in 0 until grid.width) for (x in 0 until grid.width) if (grid[x,y]) {
            val xx = x + 4; val yy = y + 4
            when (if (grid.isFunctional(x,y)) ModuleShape.SQUARE else draft.shape) {
                ModuleShape.SQUARE -> append("<rect x=\"$xx\" y=\"$yy\" width=\"1\" height=\"1\"/>")
                ModuleShape.ROUNDED -> append("<rect x=\"$xx\" y=\"$yy\" width=\"1\" height=\"1\" rx=\"0.24\"/>")
                ModuleShape.DOTS -> append("<circle cx=\"${xx + .5}\" cy=\"${yy + .5}\" r=\"0.48\"/>")
            }
        }
        append("</g></svg>")
    }
}
