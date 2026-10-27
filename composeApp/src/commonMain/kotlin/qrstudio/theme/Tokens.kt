package qrstudio.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class Tokens(val dark: Boolean=false) {
    val bg=Color(if(dark) 0xFF161514 else 0xFFF3F2F2)
    val surface=Color(if(dark) 0xFF242221 else 0xFFEAE9E9)
    val text=Color(if(dark) 0xFFF3F2F2 else 0xFF201E1D)
    val accent=Color(0xFFEC3013)
    val divider=text.copy(alpha=if(dark) .32f else .4f)
    val muted=text.copy(alpha=.6f)
    val label=text.copy(alpha=.7f)
    val accent100=Color(if(dark) 0xFF4D170E else 0xFFFFF2EF)
    val accent200=Color(0xFFFFE0D9)
    val accent300=Color(0xFFFFC4B8)
    val accent400=Color(0xFFFF9783)
    val accent500=Color(0xFFFF563C)
    val accent600=Color(0xFFDD2B0F)
    val accent700=Color(0xFFAE1800)
    val accent800=Color(if(dark) 0xFFFFC4B8 else 0xFF7C1405)
    val accent900=Color(0xFF4D170E)
    val neutral100=Color(if(dark) 0xFF3A3737 else 0xFFF8F4F4)
    val neutral200=Color(0xFFEAE7E7)
    val neutral300=Color(if(dark) 0xFF2D2B2B else 0xFFD7D3D3)
    val neutral400=Color(0xFFBAB6B6)
    val neutral500=Color(0xFF9B9797)
    val neutral600=Color(0xFF7D7979)
    val neutral700=Color(0xFF605D5D)
    val neutral800=Color(0xFF444141)
    val neutral900=Color(0xFF2D2B2B)
}
val LocalTokens=staticCompositionLocalOf { Tokens() }
val DesignPalette=listOf(0xFF201E1DL,0xFFFFFFFFL,0xFFF3F2F2L,0xFFEC3013L,0xFF1F3A5FL,0xFF2F6B4FL,0xFFF2C94CL)
