package qrstudio.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import qrstudio.theme.LocalTokens

@Composable private fun Modifier.focusRule(source: MutableInteractionSource): Modifier {
    val focused by source.collectIsFocusedAsState();val t=LocalTokens.current
    return drawWithContent { drawContent();if(focused) { val extra=3.dp.toPx();drawRect(t.accent,Offset(-extra,-extra),Size(size.width+extra*2,size.height+extra*2),style=Stroke(2.dp.toPx())) } }
}
@Composable fun PrimaryButton(label: String,icon: StudioIcon?=null,modifier: Modifier=Modifier,compact: Boolean=false,enabled: Boolean=true,onClick: ()->Unit) {
    val t=LocalTokens.current;val source=remember { MutableInteractionSource() };val pressed by source.collectIsPressedAsState()
    val fill=if(pressed) if(t.dark) t.accent400 else t.accent600 else t.accent
    Box(modifier.heightIn(min=48.dp),contentAlignment=Alignment.CenterStart) {
        Row(Modifier.fillMaxWidth().heightIn(min=if(compact) 40.dp else 44.dp).focusRule(source)
            .background(fill.copy(alpha=if(enabled) 1f else .45f)).hoverable(source)
            .clickable(source,ripple(color=t.accent),enabled,role=Role.Button,onClick=onClick).padding(horizontal=14.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            if(icon!=null) Glyph(icon,modifier=Modifier.size(18.dp),color=t.bg)
            Text(label,color=t.bg,fontSize=14.sp,fontWeight=FontWeight.ExtraBold,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}
@Composable fun SecondaryButton(label: String,icon: StudioIcon?=null,modifier: Modifier=Modifier,selected: Boolean=false,danger: Boolean=false,enabled: Boolean=true,singleLine: Boolean=false,onClick: ()->Unit) {
    val t=LocalTokens.current;val source=remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState();val hovered by source.collectIsHoveredAsState()
    val fill=if(selected) t.text else t.text.copy(alpha=if(pressed) .14f else if(hovered) .07f else 0f)
    val ink=if(selected) t.bg else if(danger) t.accent else t.text
    Row(modifier.heightIn(min=44.dp).focusRule(source).background(fill).border(1.dp,t.divider).hoverable(source)
        .clickable(source,ripple(color=t.accent),enabled,role=Role.Button,onClick=onClick).padding(horizontal=14.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        if(icon!=null) Glyph(icon,modifier=Modifier.size(18.dp),color=ink)
        Text(label,color=ink,fontWeight=FontWeight.ExtraBold,fontSize=14.sp,maxLines=if(singleLine) 1 else Int.MAX_VALUE,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}
@Composable fun StudioIconButton(icon: StudioIcon,label: String,enabled: Boolean=true,onClick: ()->Unit) {
    val t=LocalTokens.current;val source=remember { MutableInteractionSource() }
    Box(Modifier.size(48.dp).focusRule(source).clickable(source,ripple(color=t.accent),enabled,role=Role.Button,onClick=onClick),contentAlignment=Alignment.Center) { Glyph(icon,label,Modifier.size(22.dp),t.text.copy(alpha=if(enabled) 1f else .45f)) }
}
