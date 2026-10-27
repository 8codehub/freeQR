package qrstudio.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import qrstudio.model.*
import qrstudio.theme.LocalTokens
import qrstudio.ui.components.Tag

@Composable fun HintBar(state: EditorState) {
    val t=LocalTokens.current
    val label=when(state.selection) {
        Selection.Qr -> "QR code"
        Selection.None -> "Nothing selected"
        is Selection.Text -> "Text: "+state.selectedText?.text?.lineSequence()?.firstOrNull().orEmpty().ifEmpty { "Empty" }.take(18)
    }
    Row(Modifier.fillMaxWidth().heightIn(min=40.dp).drawBehind { drawLine(t.divider,Offset(0f,size.height-1.dp.toPx()),Offset(size.width,size.height-1.dp.toPx()),2.dp.toPx()) }.padding(horizontal=16.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Tag(label)
        Text("Tap a layer to select, drag to move",Modifier.weight(1f),color=t.muted,fontSize=12.sp)
    }
}
