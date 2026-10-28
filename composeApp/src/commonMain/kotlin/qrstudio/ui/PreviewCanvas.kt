package qrstudio.ui

import org.jetbrains.compose.resources.stringResource
import qrstudio.generated.resources.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import qrstudio.model.*
import qrstudio.render.*
import qrstudio.theme.LocalTokens
import qrstudio.vm.EditorViewModel

@Composable fun PreviewCanvas(state: EditorState,vm: EditorViewModel,measurer: TextMeasurer,sans: FontFamily,previewHeight: androidx.compose.ui.unit.Dp=330.dp) {
    val t=LocalTokens.current;val focus=LocalFocusManager.current
    val canvasLabel=stringResource(Res.string.canvas_description)
    val logicalW=1080f;val logicalH=state.canvasHeight.toFloat()
    val frame=remember(state,measurer,sans) { prepareDesign(state,logicalW,logicalH,measurer,sans) }
    val current by rememberUpdatedState(state);val currentFrame by rememberUpdatedState(frame)
    BoxWithConstraints(Modifier.fillMaxWidth().height(previewHeight).background(t.surface).padding(16.dp),contentAlignment=Alignment.Center) {
        val aspect=state.ratio.a.toFloat()/state.ratio.b
        val width=minOf(maxWidth,maxHeight*aspect);val height=width/aspect
        Canvas(Modifier.size(width,height).dropShadow(RectangleShape,Shadow(radius=10.dp,color=t.neutral900.copy(alpha=.16f),offset=DpOffset(0.dp,3.dp))).clipToBounds()
            .semantics { contentDescription=canvasLabel }
            .pointerInput(width,height) {
                awaitEachGesture {
                    val down=awaitFirstDown();focus.clearFocus()
                    fun logical(point: Offset)=Offset(point.x/size.width*logicalW,point.y/size.height*current.canvasHeight)
                    val first=logical(down.position)
                    val selected=hitTest(first.x,first.y,currentFrame.hits)
                    vm.select(selected)
                    if(selected!=Selection.None) {
                        val center=when(selected) { Selection.Qr -> Offset(current.qx*logicalW,current.qy*current.canvasHeight);is Selection.Text -> current.texts.first { it.id==selected.id }.let { Offset(it.x*logicalW,it.y*current.canvasHeight) };else -> first }
                        val offset=first-center
                        down.consume()
                        do {
                            val event=awaitPointerEvent()
                            event.changes.forEach { change -> if(change.pressed) {
                                val point=logical(change.position)-offset
                                vm.drag(selected,point.x/logicalW,point.y/current.canvasHeight)
                                change.consume()
                            } }
                        } while(event.changes.any { it.pressed })
                    }
                    vm.endDrag()
                }
            }) {
            val tile=16.dp.toPx()
            for(y in 0..(size.height/tile).toInt()) for(x in 0..(size.width/tile).toInt()) drawRect(if((x+y)%2==0) t.neutral300 else t.neutral100,Offset(x*tile,y*tile),Size(tile,tile))
            withTransform({ scale(size.width/logicalW,size.height/logicalH,Offset.Zero) }) { drawDesign(state,logicalW,logicalH,frame,exportMode=false) }
        }
    }
}
