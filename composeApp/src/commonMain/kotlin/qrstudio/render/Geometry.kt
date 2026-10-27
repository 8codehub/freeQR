package qrstudio.render

import kotlin.math.abs
import qrstudio.model.Selection

data class HitBox(val left: Float,val top: Float,val right: Float,val bottom: Float) {
    fun contains(x: Float,y: Float)=x>=left && x<=right && y>=top && y<=bottom
    fun expanded(x: Float,y: Float=x)=HitBox(left-x,top-y,right+x,bottom+y)
    val width get()=right-left
    val height get()=bottom-top
}
data class LayerHit(val selection: Selection,val bounds: HitBox)
fun hitTest(x: Float,y: Float,hits: List<LayerHit>): Selection = hits.asReversed().firstOrNull { it.bounds.contains(x,y) }?.selection ?: Selection.None

data class SnapResult(val x: Float,val y: Float,val vertical: Boolean,val horizontal: Boolean)
fun snapCenter(x: Float,y: Float,enabled: Boolean): SnapResult {
    val sx=enabled && abs(x-.5f)<.02f;val sy=enabled && abs(y-.5f)<.02f
    return SnapResult((if(sx) .5f else x).coerceIn(0f,1f),(if(sy) .5f else y).coerceIn(0f,1f),sx,sy)
}
data class GradientGeometry(val start: Float,val end: Float,val top: Float,val height: Float)
fun gradientGeometry(height: Float,length: Float,up: Boolean): GradientGeometry {
    val len=height*length
    return if(up) GradientGeometry(height,height-len,height-len,len) else GradientGeometry(0f,len,0f,len)
}
