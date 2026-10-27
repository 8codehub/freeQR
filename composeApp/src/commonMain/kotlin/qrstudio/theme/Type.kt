package qrstudio.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import qrstudio.generated.resources.*

@Composable fun archivoFamily()=FontFamily(
    Font(Res.font.archivo_regular,FontWeight.Normal),
    Font(Res.font.archivo_semibold,FontWeight.SemiBold),
    Font(Res.font.archivo_extrabold,FontWeight.ExtraBold)
)
