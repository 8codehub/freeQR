package qrstudio.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import qrstudio.theme.*

fun colorHex(value: Long)="#"+(value and 0xFFFFFF).toString(16).padStart(6,'0').uppercase()
@OptIn(ExperimentalLayoutApi::class)
@Composable fun Swatches(value: Long,onChange: (Long)->Unit,onCustom: ()->Unit) {
    val t=LocalTokens.current
    FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        (DesignPalette+listOf<Long?>(null)).forEach { item ->
            val custom=item==null;val selected=if(custom) value !in DesignPalette else item==value
            val color=if(custom && !selected) t.surface else Color(item ?: value)
            val name=if(custom) "Custom color" else "Color ${colorHex(item!!)}"
            Box(Modifier.size(48.dp).semantics { contentDescription=name }.selectable(selected,role=Role.RadioButton,onClick={ if(custom) onCustom() else onChange(item!!) },indication=ripple(color=t.accent),interactionSource=remember { androidx.compose.foundation.interaction.MutableInteractionSource() }),contentAlignment=Alignment.Center) {
                Box(Modifier.size(38.dp).drawBehind { if(selected) { val offset=3.dp.toPx();drawRect(t.accent,Offset(-offset,-offset),Size(size.width+offset*2,size.height+offset*2),style=Stroke(2.dp.toPx())) } }.background(color).border(1.dp,t.divider),contentAlignment=Alignment.Center) {
                    if(custom && !selected) Glyph(StudioIcon.Plus,modifier=Modifier.size(20.dp))
                }
            }
        }
    }
}
