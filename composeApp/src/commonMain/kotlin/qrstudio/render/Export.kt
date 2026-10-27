package qrstudio.render

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import qrstudio.model.EditorState
import qrstudio.platform.encodePng
import kotlin.math.roundToInt

fun renderExport(state: EditorState,measurer: TextMeasurer,sans: FontFamily): ImageBitmap {
    val width=state.exportWidth
    val height=(width.toDouble()*state.ratio.b/state.ratio.a).roundToInt()
    val bitmap=ImageBitmap(width,height)
    val frame=prepareDesign(state,width.toFloat(),height.toFloat(),measurer,sans)
    CanvasDrawScope().draw(Density(1f,1f),LayoutDirection.Ltr,Canvas(bitmap),Size(width.toFloat(),height.toFloat())) {
        drawDesign(state,width.toFloat(),height.toFloat(),frame,exportMode=true)
    }
    return bitmap
}
fun exportPng(state: EditorState,measurer: TextMeasurer,sans: FontFamily)=encodePng(renderExport(state,measurer,sans))
