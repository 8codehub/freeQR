package qrstudio.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import qrstudio.model.*
import qrstudio.qr.QrMatrix

internal fun finder(r: Int,c: Int,n: Int)=(r<7 && c<7)||(r<7 && c>=n-7)||(r>=n-7 && c<7)
private fun Path.rounded(x: Float,y: Float,width: Float,height: Float,tl: Float,tr: Float=tl,br: Float=tl,bl: Float=tl) {
    addRoundRect(RoundRect(x,y,x+width,y+height,CornerRadius(tl),CornerRadius(tr),CornerRadius(br),CornerRadius(bl)))
}
fun modulePath(matrix: QrMatrix,x0: Float,y0: Float,m: Float,style: ModuleStyle): Path=Path().apply {
    fun d(r: Int,c: Int)=matrix.isDark(r,c) && !finder(r,c,matrix.n)
    for(r in 0 until matrix.n) for(c in 0 until matrix.n) if(d(r,c)) {
        val x=x0+c*m;val y=y0+r*m
        when(style) {
            ModuleStyle.Square -> addRect(Rect(x,y,x+m+.6f,y+m+.6f))
            ModuleStyle.Rounded -> {
                val radius=.5f*m
                rounded(x,y,m+.6f,m+.6f,
                    if(!d(r-1,c)&&!d(r,c-1)) radius else 0f,
                    if(!d(r-1,c)&&!d(r,c+1)) radius else 0f,
                    if(!d(r+1,c)&&!d(r,c+1)) radius else 0f,
                    if(!d(r+1,c)&&!d(r,c-1)) radius else 0f)
            }
            ModuleStyle.Dots -> addOval(Rect(x+.08f*m,y+.08f*m,x+.92f*m,y+.92f*m))
            ModuleStyle.Diamond -> { moveTo(x+m/2,y);lineTo(x+m,y+m/2);lineTo(x+m/2,y+m);lineTo(x,y+m/2);close() }
            ModuleStyle.Lines -> rounded(x+.1f*m,y,.8f*m,m+if(d(r+1,c)) .6f else 0f,
                if(!d(r-1,c)) .4f*m else 0f,if(!d(r-1,c)) .4f*m else 0f,
                if(!d(r+1,c)) .4f*m else 0f,if(!d(r+1,c)) .4f*m else 0f)
        }
    }
}
fun eyePath(x: Float,y: Float,m: Float,style: EyeStyle): Path=Path().apply {
    fillType=PathFillType.EvenOdd
    when(style) {
        EyeStyle.Square -> { addRect(Rect(x,y,x+7*m,y+7*m));addRect(Rect(x+m,y+m,x+6*m,y+6*m));addRect(Rect(x+2*m,y+2*m,x+5*m,y+5*m)) }
        EyeStyle.Rounded -> { rounded(x,y,7*m,7*m,2*m);rounded(x+m,y+m,5*m,5*m,1.4f*m);rounded(x+2*m,y+2*m,3*m,3*m,.9f*m) }
        EyeStyle.Circle -> { addOval(Rect(x,y,x+7*m,y+7*m));addOval(Rect(x+m,y+m,x+6*m,y+6*m));addOval(Rect(x+2*m,y+2*m,x+5*m,y+5*m)) }
        EyeStyle.Leaf -> { rounded(x,y,7*m,7*m,3*m,0f,3*m,0f);rounded(x+m,y+m,5*m,5*m,2*m,0f,2*m,0f);rounded(x+2*m,y+2*m,3*m,3*m,1.3f*m,0f,1.3f*m,0f) }
    }
}
