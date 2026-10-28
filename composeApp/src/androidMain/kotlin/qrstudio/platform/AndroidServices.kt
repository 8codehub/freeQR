package qrstudio.platform

import org.jetbrains.compose.resources.getString
import qrstudio.generated.resources.*
import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.roundToInt

private val Context.editorPreferences by preferencesDataStore("qr_studio_multiplatform")
class AndroidSessionStore(private val context: Context): SessionStore {
    private val key=stringPreferencesKey("editor_state")
    override suspend fun read()=context.editorPreferences.data.first()[key]
    override suspend fun write(value: String) { context.editorPreferences.edit { it[key]=value } }
}
class AndroidImagePicker(private val activity: ComponentActivity): ImagePicker {
    override val recoveredImages=MutableSharedFlow<PickedImage>(replay=1)
    private var pending: CancellableContinuation<PickedImage?>?=null
    private val launcher=activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val continuation=pending;pending=null
        if(uri==null) { if(continuation?.isActive==true) continuation.resume(null) }
        else activity.lifecycleScope.launch {
            try {
                val result=withContext(Dispatchers.IO) {
                    val bitmap=decode(uri)
                    val dir=File(activity.cacheDir,"backgrounds").apply { mkdirs() }
                    val file=File(dir,"background-${System.currentTimeMillis()}.png")
                    file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
                    PickedImage(bitmap.asImageBitmap(),file.absolutePath)
                }
                if(continuation?.isActive==true) continuation.resume(result) else recoveredImages.emit(result)
            } catch(e: Exception) { if(continuation?.isActive==true) continuation.resumeWithException(e) }
        }
    }
    override suspend fun pick(): PickedImage?=withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            check(pending==null)
            pending=continuation
            continuation.invokeOnCancellation { if(pending===continuation) pending=null }
            try { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            catch(e: Exception) { pending=null;continuation.resumeWithException(e) }
        }
    }
    override suspend fun restore(reference: String): ImageBitmap?=withContext(Dispatchers.IO) {
        val file=File(reference)
        if(file.isFile) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
    }
    private fun decode(uri: Uri): Bitmap {
        if(Build.VERSION.SDK_INT>=28) return ImageDecoder.decodeBitmap(ImageDecoder.createSource(activity.contentResolver,uri)) { decoder,info,_ ->
            decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
            val factor=minOf(1f,4096f/max(info.size.width,info.size.height))
            decoder.setTargetSize(max(1,(info.size.width*factor).roundToInt()),max(1,(info.size.height*factor).roundToInt()))
        }
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        activity.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,bounds) }
        val options=BitmapFactory.Options().apply { var sample=1;while(max(bounds.outWidth,bounds.outHeight)/sample>4096) sample*=2;inSampleSize=sample }
        val bitmap=checkNotNull(activity.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,options) })
        val orientation=runCatching { activity.contentResolver.openInputStream(uri).use { android.media.ExifInterface(checkNotNull(it)).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,1) } }.getOrDefault(1)
        val transform=Matrix().apply { when(orientation) {
            2 -> setScale(-1f,1f);3 -> setRotate(180f);4 -> setScale(1f,-1f)
            5 -> { setRotate(90f);postScale(-1f,1f) };6 -> setRotate(90f)
            7 -> { setRotate(270f);postScale(-1f,1f) };8 -> setRotate(270f)
        } }
        return if(transform.isIdentity) bitmap else Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,transform,true).also { bitmap.recycle() }
    }
}
class AndroidImageSaver(private val activity: ComponentActivity): ImageSaver {
    private var permission: CancellableContinuation<Boolean>?=null
    private val request=activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> permission?.let { if(it.isActive) it.resume(granted) };permission=null }
    override suspend fun save(png: ByteArray,fileName: String) {
        if(Build.VERSION.SDK_INT<29 && ContextCompat.checkSelfPermission(activity,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED) {
            val granted=withContext(Dispatchers.Main.immediate) { suspendCancellableCoroutine { continuation ->
                permission=continuation;continuation.invokeOnCancellation { if(permission===continuation) permission=null };request.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } }
            if(!granted) throw IOException("Storage permission was denied")
        }
        withContext(Dispatchers.IO) {
            val resolver=activity.contentResolver
            val values=ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME,fileName);put(MediaStore.Images.Media.MIME_TYPE,"image/png")
                if(Build.VERSION.SDK_INT>=29) { put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/QR Studio/");put(MediaStore.Images.Media.IS_PENDING,1) }
                else {
                    val directory=File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"QR Studio").apply { check(exists()||mkdirs()) }
                    @Suppress("DEPRECATION") put(MediaStore.Images.Media.DATA,File(directory,fileName).absolutePath)
                }
            }
            val uri=checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values))
            try {
                checkNotNull(resolver.openOutputStream(uri)).use { it.write(png) }
                if(Build.VERSION.SDK_INT>=29) { values.clear();values.put(MediaStore.Images.Media.IS_PENDING,0);check(resolver.update(uri,values,null,null)>0) }
            } catch(e: Exception) { resolver.delete(uri,null,null);throw e }
        }
    }
}
class AndroidImageSharer(private val activity: ComponentActivity): ImageSharer {
    override suspend fun share(png: ByteArray) {
        val uri=withContext(Dispatchers.IO) {
            val directory=File(activity.cacheDir,"shared").apply { mkdirs() }
            val file=File(directory,"qr-studio.png").apply { writeBytes(png) }
            FileProvider.getUriForFile(activity,"${activity.packageName}.files",file)
        }
        val title=getString(Res.string.share_chooser)
        withContext(Dispatchers.Main.immediate) {
            val intent=Intent(Intent.ACTION_SEND).apply {
                type="image/png";putExtra(Intent.EXTRA_STREAM,uri)
                clipData=ClipData.newRawUri("QR design",uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(intent,title))
        }
    }
}
fun androidServices(activity: ComponentActivity)=PlatformServices(
    AndroidImagePicker(activity),AndroidImageSaver(activity),AndroidImageSharer(activity),AndroidSessionStore(activity.applicationContext),
    object: Haptics { override fun snap() { activity.window.decorView.performHapticFeedback(if(Build.VERSION.SDK_INT>=34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.TEXT_HANDLE_MOVE) } },
    themeChanged={ dark -> WindowCompat.getInsetsController(activity.window,activity.window.decorView).apply { isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark } }
)
