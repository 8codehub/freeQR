package qrstudio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import qrstudio.theme.LocalTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun StudioSlider(label: String,value: Float,range: ClosedFloatingPointRange<Float>,steps: Int=0,rainbow: Boolean=false,onChange: (Float)->Unit) {
    val t=LocalTokens.current
    Field(label) {
        Slider(value=value,onValueChange=onChange,valueRange=range,steps=steps,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),
            colors=SliderDefaults.colors(thumbColor=t.accent,activeTrackColor=t.accent,inactiveTrackColor=t.divider),
            thumb={ Box(Modifier.size(14.dp).background(t.accent)) },track={
                Canvas(Modifier.fillMaxWidth().height(if(rainbow) 12.dp else 2.dp)) {
                    if(rainbow) drawRect(Brush.horizontalGradient(listOf(Color.Red,Color.Yellow,Color.Green,Color.Cyan,Color.Blue,Color.Magenta,Color.Red)))
                    else { drawLine(t.divider,Offset(0f,size.height/2),Offset(size.width,size.height/2),2.dp.toPx());drawLine(t.accent,Offset(0f,size.height/2),Offset(size.width*((value-range.start)/(range.endInclusive-range.start)).coerceIn(0f,1f),size.height/2),2.dp.toPx()) }
                }
            })
    }
}
