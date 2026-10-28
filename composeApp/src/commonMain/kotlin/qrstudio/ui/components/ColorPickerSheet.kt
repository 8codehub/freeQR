package qrstudio.ui.components

import org.jetbrains.compose.resources.stringResource
import qrstudio.generated.resources.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import qrstudio.theme.LocalTokens
import kotlin.math.*

fun hsvColor(h: Float,s: Float,v: Float): Long {
    val c=v*s;val x=c*(1-abs((h/60f)%2-1));val m=v-c
    val rgb=when((h/60f).toInt()%6) { 0 -> floatArrayOf(c,x,0f);1 -> floatArrayOf(x,c,0f);2 -> floatArrayOf(0f,c,x);3 -> floatArrayOf(0f,x,c);4 -> floatArrayOf(x,0f,c);else -> floatArrayOf(c,0f,x) }
    return 0xFF000000L or (((rgb[0]+m)*255).roundToInt().toLong() shl 16) or (((rgb[1]+m)*255).roundToInt().toLong() shl 8) or ((rgb[2]+m)*255).roundToInt().toLong()
}
fun colorHsv(color: Long): FloatArray {
    val r=((color shr 16)and 255)/255f;val g=((color shr 8)and 255)/255f;val b=(color and 255)/255f
    val max=maxOf(r,g,b);val min=minOf(r,g,b);val d=max-min
    val hue=if(d==0f) 0f else when(max) { r -> ((g-b)/d%6)*60;g -> ((b-r)/d+2)*60;else -> ((r-g)/d+4)*60 }
    return floatArrayOf((hue+360)%360,if(max==0f) 0f else d/max,max)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ColorPickerSheet(value: Long,onChange: (Long)->Unit,onDismiss: ()->Unit) {
    val t=LocalTokens.current;val initial=remember { colorHsv(value) }
    var hue by remember { mutableFloatStateOf(initial[0]) };var sat by remember { mutableFloatStateOf(initial[1]) };var brightness by remember { mutableFloatStateOf(initial[2]) }
    var hex by remember(value) { mutableStateOf(colorHex(value)) }
    val currentHue by rememberUpdatedState(hue);val notify by rememberUpdatedState(onChange)
    ModalBottomSheet(onDismissRequest=onDismiss,shape=RectangleShape,containerColor=t.bg,contentColor=t.text,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),dragHandle={ Box(Modifier.padding(vertical=12.dp).size(32.dp,3.dp).background(t.divider)) }) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text(stringResource(Res.string.custom_color),Modifier.weight(1f),fontSize=20.sp);SecondaryButton(stringResource(Res.string.keyboard_done),onClick=onDismiss) }
            Canvas(Modifier.fillMaxWidth().height(180.dp).border(1.dp,t.divider).pointerInput(Unit) {
                fun update(pos: Offset) { sat=(pos.x/size.width).coerceIn(0f,1f);brightness=(1-pos.y/size.height).coerceIn(0f,1f);notify(hsvColor(currentHue,sat,brightness)) }
                awaitEachGesture { val down=awaitFirstDown();update(down.position);down.consume();do { val event=awaitPointerEvent();event.changes.forEach { if(it.pressed) { update(it.position);it.consume() } } } while(event.changes.any { it.pressed }) }
            }) {
                drawRect(Brush.horizontalGradient(listOf(Color.White,Color(hsvColor(hue,1f,1f)))))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent,Color.Black)))
                drawCircle(Color.White,7.dp.toPx(),Offset(sat*size.width,(1-brightness)*size.height),style=Stroke(2.dp.toPx()))
                drawCircle(Color.Black,9.dp.toPx(),Offset(sat*size.width,(1-brightness)*size.height),style=Stroke(1.dp.toPx()))
            }
            StudioSlider(stringResource(Res.string.hue),hue,0f..360f,rainbow=true) { hue=it%360;onChange(hsvColor(hue,sat,brightness)) }
            Field(stringResource(Res.string.hex)) { TextInput(hex,{ next ->
                if(next.length<=7 && next.removePrefix("#").all { it.isDigit()||it.lowercaseChar() in 'a'..'f' }) {
                    hex=next
                    next.removePrefix("#").takeIf { it.length==6 }?.toLongOrNull(16)?.let { rgb ->
                        val color=0xFF000000L or rgb;val hsv=colorHsv(color);hue=hsv[0];sat=hsv[1];brightness=hsv[2];onChange(color)
                    }
                }
            },placeholder="#RRGGBB",keyboard=KeyboardType.Ascii) }
        }
    }
}
