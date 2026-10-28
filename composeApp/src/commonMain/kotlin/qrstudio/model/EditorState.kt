package qrstudio.model

import org.jetbrains.compose.resources.StringResource
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import qrstudio.qr.QrMatrix

object EditorDefaults {
    const val ExportWidth = 2048
    val AllowedExportWidths = setOf(1080,2048,4096)
    const val SnapToCenter = true
}
@Serializable data class EditorState(
    val tab: EditorTab = EditorTab.Content, val theme: EditorTheme = EditorTheme.System,
    val type: ContentType = ContentType.Url, val data: String = "https://example.com",
    val ssid: String = "", val wifiPass: String = "", val phone: String = "", val ecl: Ecl = Ecl.Q,
    val modStyle: ModuleStyle = ModuleStyle.Rounded, val eyeStyle: EyeStyle = EyeStyle.Rounded,
    val fg: Long = 0xFF201E1D, val eye: Long = 0xFFEC3013, val plate: Boolean = true, val plateColor: Long = 0xFFFFFFFF,
    val qrSize: Float = .56f, val qx: Float = .5f, val qy: Float = .5f,
    val bgMode: BackgroundMode = BackgroundMode.Solid, val bg: Long = 0xFFF3F2F2,
    val bgImageReference: String? = null, @Transient val bgImage: ImageBitmap? = null,
    val ratio: CanvasRatio = CanvasRatio.Square,
    val grad: Boolean = false, val gradDir: GradientDirection = GradientDirection.Down,
    val gradColor: Long = 0xFF201E1D, val gradOpacity: Float = .75f, val gradLen: Float = .60f,
    val texts: List<TextLayer> = emptyList(), val selection: Selection = Selection.Qr,
    val exportWidth: Int = EditorDefaults.ExportWidth, val snapToCenter: Boolean = EditorDefaults.SnapToCenter,
    @Transient val matrix: QrMatrix? = null, @Transient val encodingFailed: Boolean = false,
    @Transient val snack: StringResource? = null, @Transient val exporting: Boolean = false,
    @Transient val guideX: Boolean = false, @Transient val guideY: Boolean = false
) {
    val canvasHeight: Int get() = (1080f * ratio.b / ratio.a).toInt()
    val selectedText: TextLayer? get() = (selection as? Selection.Text)?.let { selected -> texts.firstOrNull { it.id == selected.id } }
    fun sanitized() = copy(qrSize = qrSize.coerceIn(.15f,.95f), qx = qx.coerceIn(0f,1f), qy = qy.coerceIn(0f,1f),
        gradOpacity = gradOpacity.coerceIn(0f,1f),gradLen = gradLen.coerceIn(.1f,1f),
        exportWidth = exportWidth.takeIf { it in EditorDefaults.AllowedExportWidths } ?: EditorDefaults.ExportWidth,
        texts = texts.map { it.copy(x=it.x.coerceIn(0f,1f),y=it.y.coerceIn(0f,1f),size=it.size.coerceIn(.025f,.22f),weight=it.weight.takeIf { n -> n in setOf(400,600,800) } ?: 800) })
}
