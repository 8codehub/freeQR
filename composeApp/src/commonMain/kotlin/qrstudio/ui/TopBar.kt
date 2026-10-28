package qrstudio.ui

import org.jetbrains.compose.resources.stringResource
import qrstudio.generated.resources.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import qrstudio.ui.components.*
import qrstudio.theme.LocalTokens

@Composable fun TopBar(dark: Boolean,exporting: Boolean,onTheme: ()->Unit,onShare: ()->Unit,onSave: ()->Unit) {
    val t=LocalTokens.current;val saveLabel=stringResource(Res.string.save)
    // On narrow phones a long translated label would squeeze the title, so the icon goes first
    val compactSave=LocalWindowInfo.current.containerDpSize.width<400.dp && saveLabel.length>6
    Row(Modifier.fillMaxWidth().heightIn(min=60.dp).drawBehind { drawLine(t.divider,Offset(0f,size.height-1.dp.toPx()),Offset(size.width,size.height-1.dp.toPx()),2.dp.toPx()) }.padding(start=16.dp,end=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        Text("QR Studio",Modifier.weight(1f),color=t.text,fontSize=20.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=(-.4).sp,maxLines=1,softWrap=false)
        StudioIconButton(if(dark) StudioIcon.Sun else StudioIcon.Moon,stringResource(Res.string.toggle_dark_mode),onClick=onTheme)
        StudioIconButton(StudioIcon.Share2,stringResource(Res.string.share),enabled=!exporting,onClick=onShare)
        PrimaryButton(saveLabel,if(compactSave) null else StudioIcon.Download,Modifier.widthIn(min=94.dp,max=150.dp).width(IntrinsicSize.Max),compact=true,enabled=!exporting,onClick=onSave)
    }
}
