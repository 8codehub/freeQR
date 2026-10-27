package qrstudio.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import qrstudio.theme.LocalTokens

@Composable fun SegmentedControl(options: List<String>,selected: Int,onSelect: (Int)->Unit) {
    val t=LocalTokens.current
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).border(1.dp,t.divider).selectableGroup()) {
        options.forEachIndexed { index,label ->
            if(index>0) Box(Modifier.width(1.dp).fillMaxHeight().background(t.divider))
            val source=remember { MutableInteractionSource() };val hover by source.collectIsHoveredAsState();val focus by source.collectIsFocusedAsState()
            Box(Modifier.weight(1f).fillMaxHeight().heightIn(min=44.dp).background(if(selected==index) t.accent else if(hover) t.text.copy(alpha=.07f) else t.bg)
                .then(if(focus) Modifier.border(2.dp,t.accent) else Modifier).hoverable(source)
                .selectable(selected==index,interactionSource=source,indication=ripple(color=t.accent),role=Role.RadioButton,onClick={ onSelect(index) })
                .padding(horizontal=if(options.size==5) 6.dp else 12.dp,vertical=7.dp),contentAlignment=Alignment.CenterStart) {
                Text(label,color=if(selected==index) t.bg else t.text,fontSize=13.sp)
            }
        }
    }
}
