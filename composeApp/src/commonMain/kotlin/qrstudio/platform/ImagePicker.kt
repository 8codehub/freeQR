package qrstudio.platform

import androidx.compose.ui.graphics.ImageBitmap

data class PickedImage(val bitmap: ImageBitmap,val reference: String)
expect interface ImagePicker {
    val recoveredImages: kotlinx.coroutines.flow.Flow<PickedImage>
    suspend fun pick(): PickedImage?
    suspend fun restore(reference: String): ImageBitmap?
}
interface SessionStore {
    suspend fun read(): String?
    suspend fun write(value: String)
}
data class PlatformServices(val picker: ImagePicker,val saver: ImageSaver,val sharer: ImageSharer,val store: SessionStore,val haptics: Haptics,val themeChanged: (Boolean)->Unit = {})
expect fun currentTimeMillis(): Long
