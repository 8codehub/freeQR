package qrstudio.ui

import org.jetbrains.compose.resources.stringResource
import qrstudio.generated.resources.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import qrstudio.model.EditorTab
import qrstudio.theme.LocalTokens
import qrstudio.ui.components.*

@Composable fun BottomNav(tab: EditorTab,onTab: (EditorTab)->Unit) {
    val t=LocalTokens.current
    val items=listOf(Triple(EditorTab.Content,StudioIcon.Link,stringResource(Res.string.tab_content)),Triple(EditorTab.QrStyle,StudioIcon.QrCode,stringResource(Res.string.tab_qr_style)),Triple(EditorTab.Background,StudioIcon.Image,stringResource(Res.string.tab_background)),Triple(EditorTab.Gradient,StudioIcon.Blend,stringResource(Res.string.tab_gradient)),Triple(EditorTab.Text,StudioIcon.Type,stringResource(Res.string.tab_text)))
    Row(Modifier.fillMaxWidth().testTag("bottom-navigation").background(t.bg).drawBehind { drawLine(t.divider,Offset.Zero,Offset(size.width,0f),2.dp.toPx()) }.navigationBarsPadding()) {
        items.forEach { (value,icon,label) ->
            val active=value==tab
            Column(Modifier.weight(1f).heightIn(min=72.dp).selectable(active,role=Role.Tab,interactionSource=androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },indication=ripple(color=t.accent),onClick={ onTab(value) }).padding(top=10.dp,bottom=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(56.dp,30.dp).background(if(active) t.accent100 else androidx.compose.ui.graphics.Color.Transparent),contentAlignment=Alignment.Center) { Glyph(icon,modifier=Modifier.size(22.dp),color=if(active) t.accent else t.label) }
                Text(label,color=if(active) t.accent else t.label,fontWeight=FontWeight.SemiBold,fontSize=11.sp)
            }
        }
    }
}
