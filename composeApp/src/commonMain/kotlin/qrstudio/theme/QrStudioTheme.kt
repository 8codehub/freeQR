package qrstudio.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable fun QrStudioTheme(dark: Boolean,content: @Composable ()->Unit) {
    val tokens=remember(dark) { Tokens(dark) }
    val font=archivoFamily()
    val type=TextStyle(fontFamily=font,color=tokens.text,fontSize=14.sp)
    val colors=if(dark) darkColorScheme(primary=tokens.accent,background=tokens.bg,surface=tokens.surface,onSurface=tokens.text) else lightColorScheme(primary=tokens.accent,background=tokens.bg,surface=tokens.surface,onSurface=tokens.text)
    CompositionLocalProvider(LocalTokens provides tokens) {
        MaterialTheme(colorScheme=colors,shapes=Shapes(RoundedCornerShape(0.dp),RoundedCornerShape(0.dp),RoundedCornerShape(0.dp),RoundedCornerShape(0.dp),RoundedCornerShape(0.dp)),
            typography=Typography(bodyLarge=type,bodyMedium=type,bodySmall=type.copy(fontSize=12.sp),labelLarge=type.copy(fontWeight=FontWeight.ExtraBold),titleLarge=type.copy(fontSize=20.sp,fontWeight=FontWeight.ExtraBold)),content=content)
    }
}
