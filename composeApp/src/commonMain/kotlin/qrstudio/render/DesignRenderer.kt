package qrstudio.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import qrstudio.model.*
import kotlin.math.max
import kotlin.math.roundToInt

data class PreparedText(val layer: TextLayer,val layout: TextLayoutResult,val origin: Offset)
data class DesignFrame(val hits: List<LayerHit>,val texts: List<PreparedText>)
@OptIn(ExperimentalTextApi::class)
fun prepareDesign(state: EditorState,width: Float,height: Float,measurer: TextMeasurer,sans: FontFamily): DesignFrame {
    val hits=mutableListOf<LayerHit>()
    state.matrix?.let { matrix ->
        val side=state.qrSize*minOf(width,height);val pad=if(state.plate) 2*side/matrix.n else 0f
        val left=state.qx*width-side/2;val top=state.qy*height-side/2
        hits+=LayerHit(Selection.Qr,HitBox(left-pad,top-pad,left+side+pad,top+side+pad))
    }
    val texts=state.texts.map { layer ->
        val family=when(layer.font) { TextFont.Sans -> sans;TextFont.Serif -> FontFamily.Serif;TextFont.Mono -> FontFamily.Monospace }
        val fs=layer.size*width
        val result=measurer.measure(AnnotatedString(layer.text),style=TextStyle(fontFamily=family,fontWeight=FontWeight(layer.weight),fontSize=fs.sp,lineHeight=(fs*1.15f).sp,textAlign=TextAlign.Center,color=Color(layer.color)),
            softWrap=false,density=Density(1f,1f),layoutDirection=LayoutDirection.Ltr)
        val origin=Offset(layer.x*width-result.size.width/2f,layer.y*height-result.size.height/2f)
        hits+=LayerHit(Selection.Text(layer.id),HitBox(origin.x,origin.y,origin.x+result.size.width,origin.y+result.size.height).expanded(12f*width/1080f,8f*width/1080f))
        PreparedText(layer,result,origin)
    }
    return DesignFrame(hits,texts)
}
@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawDesign(state: EditorState,width: Float,height: Float,frame: DesignFrame,exportMode: Boolean) {
    clipRect(0f,0f,width,height) {
        // A newly created export bitmap is transparent. Do not clear preview's checkerboard.
        when(state.bgMode) {
            BackgroundMode.Solid -> drawRect(Color(state.bg),size=Size(width,height))
            BackgroundMode.Image -> state.bgImage?.let { image ->
                val factor=max(width/image.width,height/image.height)
                withTransform({ translate((width-image.width*factor)/2,(height-image.height*factor)/2);scale(factor,factor,Offset.Zero) }) { drawImage(image,filterQuality=FilterQuality.High) }
            }
            BackgroundMode.Transparent -> Unit
        }
        if(state.grad && state.gradOpacity>0f) {
            val geometry=gradientGeometry(height,state.gradLen,state.gradDir==GradientDirection.Up)
            val brush=Brush.verticalGradient(listOf(Color(state.gradColor).copy(alpha=state.gradOpacity),Color(state.gradColor).copy(alpha=0f)),startY=geometry.start,endY=geometry.end)
            drawRect(brush,topLeft=Offset(0f,geometry.top),size=Size(width,geometry.height))
        }
        state.matrix?.let { matrix ->
            val side=state.qrSize*minOf(width,height);val m=side/matrix.n
            val x=state.qx*width-side/2;val y=state.qy*height-side/2;val pad=if(state.plate) 2*m else 0f
            if(state.plate) drawRect(Color(state.plateColor),Offset(x-pad,y-pad),Size(side+2*pad,side+2*pad))
            drawPath(modulePath(matrix,x,y,m,state.modStyle),Color(state.fg))
            listOf(Offset(x,y),Offset(x+(matrix.n-7)*m,y),Offset(x,y+(matrix.n-7)*m)).forEach { drawPath(eyePath(it.x,it.y,m,state.eyeStyle),Color(state.eye)) }
        }
        frame.texts.forEach { drawText(it.layout,topLeft=it.origin) }
        if(!exportMode) {
            frame.hits.firstOrNull { it.selection==state.selection }?.bounds?.expanded(6f)?.let { box ->
                drawRect(Color(0xFFEC3013),Offset(box.left,box.top),Size(box.width,box.height),style=Stroke(4f,pathEffect=PathEffect.dashPathEffect(floatArrayOf(14f,10f))))
            }
            if(state.guideX) drawLine(Color(0xFFEC3013).copy(alpha=.8f),Offset(width/2,0f),Offset(width/2,height),2f)
            if(state.guideY) drawLine(Color(0xFFEC3013).copy(alpha=.8f),Offset(0f,height/2),Offset(width,height/2),2f)
        }
    }
}
