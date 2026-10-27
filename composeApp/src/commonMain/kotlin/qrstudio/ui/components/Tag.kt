package qrstudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import qrstudio.theme.LocalTokens

@Composable fun Tag(label: String) { val t=LocalTokens.current;Text(label,Modifier.background(t.accent100).padding(horizontal=10.dp,vertical=3.dp),color=t.accent800,fontSize=11.sp,letterSpacing=.22.sp) }
