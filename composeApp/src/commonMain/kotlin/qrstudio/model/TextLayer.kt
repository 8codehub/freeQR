package qrstudio.model

import kotlinx.serialization.Serializable
@Serializable data class TextLayer(
    val id: Long, val text: String = "Scan me", val x: Float = .5f, val y: Float = .12f,
    val size: Float = .07f, val color: Long = 0xFF201E1D, val font: TextFont = TextFont.Sans, val weight: Int = 800
)
