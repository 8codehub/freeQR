package qrstudio.platform

import androidx.compose.ui.graphics.ImageBitmap
expect interface ImageSharer { suspend fun share(png: ByteArray) }
expect fun encodePng(image: ImageBitmap): ByteArray
expect fun exportFileName(): String
