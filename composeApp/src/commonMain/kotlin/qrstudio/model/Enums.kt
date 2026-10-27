package qrstudio.model

import kotlinx.serialization.Serializable
@Serializable enum class EditorTab { Content, QrStyle, Background, Gradient, Text }
@Serializable enum class EditorTheme { System, Light, Dark }
@Serializable enum class ContentType { Url, Text, Wifi, Phone }
@Serializable enum class Ecl { L, M, Q, H }
@Serializable enum class ModuleStyle { Square, Rounded, Dots, Diamond, Lines }
@Serializable enum class EyeStyle { Square, Rounded, Circle, Leaf }
@Serializable enum class BackgroundMode { Transparent, Solid, Image }
@Serializable enum class GradientDirection { Down, Up }
@Serializable enum class TextFont { Sans, Serif, Mono }
@Serializable enum class CanvasRatio(val a: Int, val b: Int) { Square(1,1), Portrait(4,5), Story(9,16), Wide(16,9) }
@Serializable sealed interface Selection {
    @Serializable data object Qr : Selection
    @Serializable data class Text(val id: Long) : Selection
    @Serializable data object None : Selection
}
