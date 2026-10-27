@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)
package qrstudio.platform

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import okio.Path.Companion.toPath
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.Foundation.*
import platform.Photos.*
import platform.PhotosUI.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min

actual interface ImagePicker {
    actual val recoveredImages: Flow<PickedImage>
    actual suspend fun pick(): PickedImage?
    actual suspend fun restore(reference: String): ImageBitmap?
}
actual interface ImageSaver { actual suspend fun save(png: ByteArray,fileName: String) }
actual interface ImageSharer { actual suspend fun share(png: ByteArray) }
actual interface Haptics { actual fun snap() }
actual fun currentTimeMillis()=(NSDate().timeIntervalSince1970*1000).toLong()
actual fun exportFileName(): String {
    val formatter=NSDateFormatter().apply { locale=NSLocale.localeWithLocaleIdentifier("en_US_POSIX");dateFormat="yyyyMMdd-HHmmss" }
    return "qr-studio-${formatter.stringFromDate(NSDate())}.png"
}
actual fun encodePng(image: ImageBitmap)=checkNotNull(Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)).bytes
private fun ByteArray.nsData(): NSData=usePinned { NSData.create(bytes=it.addressOf(0),length=size.toULong()) }
private fun NSData.byteArray(): ByteArray {
    require(length>0u)
    return ByteArray(length.toInt()).also { result -> result.usePinned { memcpy(it.addressOf(0),bytes,length) } }
}
private fun cacheDirectory(): String {
    val path=NSSearchPathForDirectoriesInDomains(NSCachesDirectory,NSUserDomainMask,true).first() as String
    val directory="$path/QR Studio"
    NSFileManager.defaultManager.createDirectoryAtPath(directory,true,null,null)
    return directory
}
private fun presenter(): UIViewController {
    var controller=checkNotNull(UIApplication.sharedApplication.keyWindow?.rootViewController)
    while(controller.presentedViewController!=null) controller=controller.presentedViewController!!
    return controller
}
class IosImagePicker: ImagePicker {
    override val recoveredImages=MutableSharedFlow<PickedImage>()
    private var retained: NSObject?=null
    override suspend fun pick(): PickedImage?=withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val configuration=PHPickerConfiguration().apply { selectionLimit=1;filter=PHPickerFilter.imagesFilter }
            val controller=PHPickerViewController(configuration=configuration)
            val delegate=object: NSObject(),PHPickerViewControllerDelegateProtocol {
                override fun picker(picker: PHPickerViewController,didFinishPicking: List<*>) {
                    picker.dismissViewControllerAnimated(true,null)
                    val result=didFinishPicking.firstOrNull() as? PHPickerResult
                    if(result==null) { if(continuation.isActive) continuation.resume(null);retained=null;return }
                    val provider=result.itemProvider
                    val type=provider.registeredTypeIdentifiers.firstOrNull() as? String ?: "public.image"
                    provider.loadDataRepresentationForTypeIdentifier(type) { data,error ->
                        if(error!=null || data==null) { if(continuation.isActive) continuation.resumeWithException(IllegalStateException(error?.localizedDescription ?: "Image unavailable"));retained=null }
                        else {
                            val image=UIImage(data=data)
                            if(image==null) { if(continuation.isActive) continuation.resumeWithException(IllegalStateException("Unsupported image"));retained=null }
                            else {
                                val width=image.size.useContents { width };val height=image.size.useContents { height }
                                val factor=min(1.0,4096.0/max(width,height))
                                val normalizedSize=platform.CoreGraphics.CGSizeMake(width*factor,height*factor)
                                UIGraphicsBeginImageContextWithOptions(normalizedSize,false,1.0)
                                image.drawInRect(CGRectMake(0.0,0.0,width*factor,height*factor))
                                val normalized=UIGraphicsGetImageFromCurrentImageContext()
                                val encoded=normalized?.let { UIImagePNGRepresentation(it) }
                                UIGraphicsEndImageContext()
                                if(encoded==null) { if(continuation.isActive) continuation.resumeWithException(IllegalStateException("Image conversion failed")) }
                                else {
                                    val path="${cacheDirectory()}/background-${currentTimeMillis()}.png"
                                    if(encoded.writeToFile(path,true)) {
                                        val picked=PickedImage(Image.makeFromEncoded(encoded.byteArray()).toComposeImageBitmap(),path)
                                        if(continuation.isActive) continuation.resume(picked)
                                    } else if(continuation.isActive) continuation.resumeWithException(IllegalStateException("Image cache unavailable"))
                                }
                                retained=null
                            }
                        }
                    }
                }
            }
            retained=delegate;controller.delegate=delegate
            continuation.invokeOnCancellation { controller.dismissViewControllerAnimated(true,null);retained=null }
            presenter().presentViewController(controller,true,null)
        }
    }
    override suspend fun restore(reference: String): ImageBitmap?=withContext(Dispatchers.Default) {
        NSData.dataWithContentsOfFile(reference)?.let { Image.makeFromEncoded(it.byteArray()).toComposeImageBitmap() }
    }
}
class IosImageSaver: ImageSaver {
    override suspend fun save(png: ByteArray,fileName: String) {
        val authorized=suspendCancellableCoroutine<Boolean> { continuation ->
            PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelAddOnly) { status ->
                if(continuation.isActive) continuation.resume(status==PHAuthorizationStatusAuthorized || status==PHAuthorizationStatusLimited)
            }
        }
        check(authorized) { "Photo Library access was denied" }
        suspendCancellableCoroutine<Unit> { continuation ->
            PHPhotoLibrary.sharedPhotoLibrary().performChanges({
                PHAssetCreationRequest.creationRequestForAsset().addResourceWithType(PHAssetResourceTypePhoto,png.nsData(),PHAssetResourceCreationOptions().apply { originalFilename=fileName })
            },completionHandler={ success,error ->
                if(continuation.isActive) { if(success) continuation.resume(Unit) else continuation.resumeWithException(IllegalStateException(error?.localizedDescription ?: "Photo save failed")) }
            })
        }
    }
}
class IosImageSharer: ImageSharer {
    override suspend fun share(png: ByteArray)=withContext(Dispatchers.Main) {
        val path="${cacheDirectory()}/qr-studio.png"
        check(png.nsData().writeToFile(path,true))
        val root=presenter()
        val controller=UIActivityViewController(activityItems=listOf(NSURL.fileURLWithPath(path)),applicationActivities=null)
        controller.popoverPresentationController?.let { popover ->
            popover.sourceView=root.view
            popover.sourceRect=CGRectMake(root.view.bounds.useContents { size.width/2 },root.view.bounds.useContents { size.height/2 },0.0,0.0)
        }
        root.presentViewController(controller,true,null)
    }
}
private val iosPreferences by lazy {
    val base=NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory,NSUserDomainMask,true).first() as String
    NSFileManager.defaultManager.createDirectoryAtPath(base,true,null,null)
    PreferenceDataStoreFactory.createWithPath(produceFile={ "$base/qr-studio.preferences_pb".toPath() })
}
class IosSessionStore: SessionStore {
    private val key=stringPreferencesKey("editor_state")
    override suspend fun read()=iosPreferences.data.first()[key]
    override suspend fun write(value: String) { iosPreferences.edit { it[key]=value } }
}

fun iosServices()=PlatformServices(IosImagePicker(),IosImageSaver(),IosImageSharer(),IosSessionStore(),object: Haptics {
    override fun snap() { UISelectionFeedbackGenerator().selectionChanged() }
},themeChanged={ dark -> UIApplication.sharedApplication.keyWindow?.overrideUserInterfaceStyle=if(dark) UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight })
