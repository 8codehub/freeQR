package qrstudio.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow

actual interface ImagePicker {
    actual val recoveredImages: Flow<PickedImage>
    actual suspend fun pick(): PickedImage?
    actual suspend fun restore(reference: String): ImageBitmap?
}
actual interface ImageSaver { actual suspend fun save(png: ByteArray,fileName: String) }
actual interface ImageSharer { actual suspend fun share(png: ByteArray) }
actual interface Haptics { actual fun snap() }
actual fun currentTimeMillis(): Long=System.currentTimeMillis()
actual fun exportFileName()="qr-studio-${SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US).format(Date())}.png"
actual fun encodePng(image: ImageBitmap): ByteArray=ByteArrayOutputStream().use { out ->
    check(image.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));out.toByteArray()
}
