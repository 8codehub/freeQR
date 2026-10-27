package qrstudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import qrstudio.theme.LocalTokens

@Composable fun EditorSnackbar(message: String,modifier: Modifier=Modifier) {
    val t=LocalTokens.current
    Text(message,modifier.padding(horizontal=16.dp).dropShadow(RectangleShape,Shadow(radius=32.dp,color=t.neutral900.copy(alpha=.22f),offset=DpOffset(0.dp,12.dp)))
        .background(t.text).padding(horizontal=16.dp,vertical=14.dp).semantics { liveRegion=LiveRegionMode.Polite },color=t.bg,fontSize=14.sp)
}
