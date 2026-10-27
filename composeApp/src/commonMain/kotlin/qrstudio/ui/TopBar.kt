package qrstudio.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import qrstudio.ui.components.*
import qrstudio.theme.LocalTokens

@Composable fun TopBar(dark: Boolean,exporting: Boolean,onTheme: ()->Unit,onShare: ()->Unit,onSave: ()->Unit) {
    val t=LocalTokens.current
    Row(Modifier.fillMaxWidth().heightIn(min=60.dp).drawBehind { drawLine(t.divider,Offset(0f,size.height-1.dp.toPx()),Offset(size.width,size.height-1.dp.toPx()),2.dp.toPx()) }.padding(start=16.dp,end=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        Text("QR Studio",Modifier.weight(1f),color=t.text,fontSize=20.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=(-.4).sp)
        StudioIconButton(if(dark) StudioIcon.Sun else StudioIcon.Moon,"Toggle dark mode",onClick=onTheme)
        StudioIconButton(StudioIcon.Share2,"Share",enabled=!exporting,onClick=onShare)
        PrimaryButton("Save",StudioIcon.Download,Modifier.widthIn(min=94.dp,max=120.dp),compact=true,enabled=!exporting,onClick=onSave)
    }
}
