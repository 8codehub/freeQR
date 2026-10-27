package com.qr.domain

import kotlin.math.max

enum class CanvasRatio(val aspect: Float) { SQUARE(1f), PORTRAIT(.8f), STORY(9f / 16f) }
enum class QrFrame { NONE, OUTLINE, DOUBLE, CARD }
data class PosterDesign(
    val enabled: Boolean = false, val photoUri: String? = null,
    val ratio: CanvasRatio = CanvasRatio.PORTRAIT,
    val gradientEnabled: Boolean = true, val topColor: Long = 0xFF203E37,
    val bottomColor: Long = 0xFFB2C794, val opacity: Float = .65f,
    val reversed: Boolean = false, val photoZoom: Float = 1f,
    val qrScale: Float = .48f, val x: Float = .5f, val y: Float = .65f,
    val frame: QrFrame = QrFrame.CARD, val caption: String = ""
) {
    fun normalized() = copy(
        photoUri = photoUri?.takeIf { it.startsWith("content://") },
        topColor = 0xFF000000L or (topColor and 0xFFFFFF), bottomColor = 0xFF000000L or (bottomColor and 0xFFFFFF),
        opacity = opacity.finite(.65f).coerceIn(0f, 1f), photoZoom = photoZoom.finite(1f).coerceIn(1f, 3f),
        qrScale = qrScale.finite(.48f).coerceIn(.26f, .76f),
        x = x.finite(.5f).coerceIn(0f, 1f), y = y.finite(.65f).coerceIn(0f, 1f), caption = caption.take(80)
    )
    private fun Float.finite(fallback: Float) = if (isFinite()) this else fallback
}
