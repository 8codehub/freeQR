package qrstudio.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import qrstudio.model.*
import qrstudio.platform.*
import qrstudio.theme.QrStudioTheme
import qrstudio.theme.archivoFamily
import qrstudio.ui.EditorScreen
import qrstudio.vm.EditorViewModel
import java.io.File

// Renders the Play Store listing images: ./gradlew :androidApp:testDebugUnitTest --tests 'qrstudio.screenshots.*'
// Output lands in play-store-screenshots/<device>/. Each class's qualifiers make 1dp map to whole pixels of the target size.

/** Target image size in dp, the dp size the app is laid out at inside the device frame, and whether it is a landscape listing. */
data class StoreDevice(val folder: String,val width: Dp,val height: Dp,val appWidth: Dp,val appHeight: Dp,val landscape: Boolean=false)

@RunWith(RobolectricTestRunner::class) @GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=[34],qualifiers="w360dp-h700dp-port-xxhdpi")
class PhoneStoreScreenshots: StoreScreenshots(StoreDevice("phone",360.dp,640.dp,392.dp,820.dp))

@RunWith(RobolectricTestRunner::class) @GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=[34],qualifiers="w600dp-h1020dp-port-xhdpi")
class Tablet7StoreScreenshots: StoreScreenshots(StoreDevice("tablet-7in",600.dp,960.dp,600.dp,960.dp))

@RunWith(RobolectricTestRunner::class) @GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=[34],qualifiers="w800dp-h1340dp-port-xhdpi")
class Tablet10StoreScreenshots: StoreScreenshots(StoreDevice("tablet-10in",800.dp,1280.dp,800.dp,1280.dp))

@RunWith(RobolectricTestRunner::class) @GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=[34],qualifiers="w1280dp-h780dp-land-hdpi")
class ChromebookStoreScreenshots: StoreScreenshots(StoreDevice("chromebook",1280.dp,720.dp,1280.dp,800.dp,landscape=true))

internal val Ink=Color(0xFF201E1D);internal val Paper=Color(0xFFF3F2F2);internal val Accent=Color(0xFFEC3013)

internal class Scene(val file: String,val headline: String,val sub: String,val backdrop: Color,val state: EditorState,val photo: Boolean=false)

internal val scenes=listOf(
    Scene("01-design","Design QR codes\nthat stand out","Shapes, colors, text and photos in one editor",Accent,
        EditorState(theme=EditorTheme.Light,tab=EditorTab.QrStyle,data="https://yourbrand.com",ecl=Ecl.H,texts=listOf(TextLayer(1,"SCAN ME",y=.1f,size=.075f)),qrSize=.6f,qy=.55f)),
    Scene("02-shapes","Shapes for\nevery style","Square, fluid, dots, diamond or lines",Ink,
        EditorState(theme=EditorTheme.Light,tab=EditorTab.QrStyle,modStyle=ModuleStyle.Dots,eyeStyle=EyeStyle.Circle,fg=0xFF1F3A5F,eye=0xFFEC3013,data="https://yourbrand.com/menu")),
    Scene("03-colors","Match your\nbrand colors","Pick from the palette or any custom color",Paper,
        EditorState(theme=EditorTheme.Light,tab=EditorTab.Background,bg=0xFFF2C94C,fg=0xFF2F6B4F,eye=0xFF201E1D,eyeStyle=EyeStyle.Leaf,data="https://yourbrand.com/shop",
            texts=listOf(TextLayer(1,"Shop the drop",y=.1f,size=.07f,color=0xFF2F6B4F)),qy=.55f)),
    Scene("04-photo","Put your code\non any photo","Use an image from your gallery as the background",Accent,
        EditorState(theme=EditorTheme.Light,tab=EditorTab.Background,bgMode=BackgroundMode.Image,ecl=Ecl.H,grad=true,gradOpacity=.6f,data="https://yourbrand.com/events",
            texts=listOf(TextLayer(1,"Summer Fest",y=.12f,size=.09f,color=0xFFFFFFFF))),photo=true),
    Scene("05-gradient","Gradients keep\nit readable","Fade the photo where your code and text sit",Ink,
        EditorState(theme=EditorTheme.Light,tab=EditorTab.Gradient,bgMode=BackgroundMode.Image,ecl=Ecl.H,grad=true,gradDir=GradientDirection.Up,gradOpacity=.85f,gradLen=.7f,
            gradColor=0xFF201E1D,qy=.44f,qrSize=.5f,data="https://yourbrand.com/tickets",texts=listOf(TextLayer(1,"Get tickets",y=.86f,size=.08f,color=0xFFFFFFFF))),photo=true),
    Scene("06-text","Add a call\nto action","Text layers with fonts, weights and colors",Paper,
        EditorState(theme=EditorTheme.Light,tab=EditorTab.Text,ratio=CanvasRatio.Portrait,bg=0xFFFFFFFF,eye=0xFFEC3013,qy=.52f,data="https://yourbrand.com/offer",
            texts=listOf(TextLayer(1,"Scan for 20% off",y=.1f,size=.08f),TextLayer(2,"Valid through Sunday",y=.9f,size=.04f,weight=400,font=TextFont.Serif,color=0xFFEC3013)),selection=Selection.Text(1))),
    Scene("07-wifi","Share Wi-Fi\nin one scan","Guests join your network without typing",Accent,
        EditorState(theme=EditorTheme.Light,tab=EditorTab.Content,type=ContentType.Wifi,ssid="Cafe Guest",wifiPass="espresso2026",bg=0xFF1F3A5F,fg=0xFF1F3A5F,eye=0xFFEC3013,
            modStyle=ModuleStyle.Rounded,texts=listOf(TextLayer(1,"Free Wi-Fi",y=.1f,size=.08f,color=0xFFFFFFFF)),qy=.55f)),
    Scene("08-phone","Links, text\nand phone numbers","Every QR type you need, ready to scan",Ink,
        EditorState(theme=EditorTheme.Light,tab=EditorTab.Content,type=ContentType.Phone,phone="+1 555 014 2678",bg=0xFF2F6B4F,fg=0xFF2F6B4F,eye=0xFF201E1D,
            modStyle=ModuleStyle.Diamond,texts=listOf(TextLayer(1,"Call us",y=.1f,size=.08f,color=0xFFFFFFFF)),qy=.55f)),
    Scene("09-story","Sized for\nstories and posts","Square, 4:5, 9:16 story or 16:9 canvas",Paper,
        EditorState(theme=EditorTheme.Light,tab=EditorTab.Background,ratio=CanvasRatio.Story,bg=0xFF201E1D,fg=0xFF201E1D,eye=0xFFEC3013,qrSize=.62f,data="https://yourbrand.com/follow",
            texts=listOf(TextLayer(1,"Follow us",y=.2f,size=.1f,color=0xFFFFFFFF),TextLayer(2,"@yourbrand",y=.8f,size=.06f,weight=600,color=0xFFEC3013)))),
    Scene("10-dark","Beautiful\ndark mode","Easy on the eyes, day or night",Ink,
        EditorState(theme=EditorTheme.Dark,tab=EditorTab.QrStyle,modStyle=ModuleStyle.Rounded,eyeStyle=EyeStyle.Leaf,bg=0xFFFFE0D9,fg=0xFF4D170E,eye=0xFFEC3013,data="https://yourbrand.com",
            texts=listOf(TextLayer(1,"Scan me",y=.1f,size=.075f,color=0xFF4D170E)),qy=.55f)),
    Scene("11-lines","Bold looks with\nlines and diamonds","Make a code people want to scan",Accent,
        EditorState(theme=EditorTheme.Light,tab=EditorTab.QrStyle,modStyle=ModuleStyle.Lines,eyeStyle=EyeStyle.Square,ratio=CanvasRatio.Wide,bg=0xFFEC3013,fg=0xFF201E1D,eye=0xFF201E1D,
            qx=.75f,qrSize=.62f,data="https://yourbrand.com/app",texts=listOf(TextLayer(1,"Get\nthe app",x=.27f,y=.5f,size=.1f,color=0xFFFFFFFF)))),
    Scene("12-export","Save or share\nin high resolution","Export crisp PNGs up to 4096 px, transparent too",Paper,
        EditorState(theme=EditorTheme.Light,tab=EditorTab.Content,type=ContentType.Url,data="https://yourbrand.com/menu",ecl=Ecl.H,modStyle=ModuleStyle.Rounded,eyeStyle=EyeStyle.Circle,
            fg=0xFF201E1D,eye=0xFFEC3013,texts=listOf(TextLayer(1,"Our menu",y=.1f,size=.08f)),qy=.55f)),
)

internal object NoServices: ImagePicker,ImageSaver,ImageSharer,SessionStore,Haptics {
    override val recoveredImages=emptyFlow<PickedImage>()
    override suspend fun pick(): PickedImage?=null
    override suspend fun restore(reference: String): ImageBitmap?=null
    override suspend fun save(png: ByteArray,fileName: String) {}
    override suspend fun share(png: ByteArray) {}
    override suspend fun read(): String?=null
    override suspend fun write(value: String) {}
    override fun snap() {}
}

/** A sunset with soft lights, drawn in code so the listing never depends on a stock photo. */
internal fun samplePhoto(): ImageBitmap {
    val bitmap=Bitmap.createBitmap(1080,1920,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap)
    canvas.drawPaint(Paint().apply { shader=LinearGradient(0f,0f,0f,1920f,intArrayOf(0xFF2B1B54.toInt(),0xFFB83B5E.toInt(),0xFFF08A4B.toInt(),0xFFF9D976.toInt()),floatArrayOf(0f,.45f,.75f,1f),Shader.TileMode.CLAMP) })
    listOf(Triple(220f,420f,260f),Triple(860f,300f,200f),Triple(540f,1500f,420f),Triple(120f,1200f,180f),Triple(940f,1000f,240f)).forEach { (x,y,r) ->
        canvas.drawCircle(x,y,r,Paint().apply { shader=RadialGradient(x,y,r,intArrayOf(0x88FFFFFF.toInt(),0x00FFFFFF),null,Shader.TileMode.CLAMP) })
    }
    return bitmap.asImageBitmap()
}

abstract class StoreScreenshots(private val device: StoreDevice) {
    @get:Rule val compose=createComposeRule()

    @Test fun render() {
        val out=File(System.getProperty("storeScreenshotsDir") ?: "build/play-store-screenshots",device.folder).apply { mkdirs() }
        val photo=samplePhoto()
        var index by mutableIntStateOf(0)
        val models=scenes.map { scene -> EditorViewModel(PlatformServices(NoServices,NoServices,NoServices,NoServices,NoServices)) }
        compose.setContent { key(index) { StoreFrame(scenes[index],models[index],device) } }
        scenes.forEachIndexed { i,scene ->
            val vm=models[i]
            vm.edit { scene.state.copy(bgImage=if(scene.photo) photo else null) }
            index=i
            // Encoding runs on Dispatchers.Default and posts back to the paused Robolectric main looper
            repeat(200) { if(vm.state.value.matrix==null) { ShadowLooper.idleMainLooper();Thread.sleep(25) } }
            check(vm.state.value.matrix!=null) { "QR for ${scene.file} was not encoded" }
            compose.waitForIdle()
            val image=compose.onNodeWithTag("store-frame").captureToImage().asAndroidBitmap()
            File(out,"${scene.file}.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }
        }
    }
}

@Composable private fun StoreFrame(scene: Scene,vm: EditorViewModel,device: StoreDevice) {
    val font=archivoFamily()
    val onBackdrop=if(scene.backdrop==Paper) Ink else Paper
    val headlineSize=if(device.width<500.dp) 34.sp else if(device.landscape) 44.sp else 52.sp
    Box(Modifier.requiredSize(device.width,device.height).background(scene.backdrop).testTag("store-frame")) {
        val caption: @Composable (Modifier,TextAlign)->Unit={ modifier,align ->
            Column(modifier,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(scene.headline,color=onBackdrop,fontFamily=font,fontWeight=FontWeight.ExtraBold,fontSize=headlineSize,lineHeight=headlineSize*1.08f,textAlign=align,modifier=Modifier.fillMaxWidth())
                Text(scene.sub,color=onBackdrop.copy(alpha=.78f),fontFamily=font,fontSize=headlineSize*.42f,lineHeight=headlineSize*.56f,textAlign=align,modifier=Modifier.fillMaxWidth())
            }
        }
        if(device.landscape) Row(Modifier.fillMaxSize().padding(horizontal=48.dp,vertical=32.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(40.dp)) {
            caption(Modifier.weight(.3f),TextAlign.Start)
            DeviceScreen(scene,vm,device,Modifier.weight(.7f).fillMaxHeight())
        } else Column(Modifier.fillMaxSize().padding(top=if(device.width<500.dp) 36.dp else 64.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            caption(Modifier.padding(horizontal=28.dp),TextAlign.Center)
            Spacer(Modifier.height(if(device.width<500.dp) 28.dp else 48.dp))
            // The device bleeds off the bottom edge, as in most store listings
            DeviceScreen(scene,vm,device,Modifier.fillMaxWidth(.82f).weight(1f).offset(y=24.dp))
        }
    }
}

/** Lays the editor out at the device's real dp size and scales it into the space the frame leaves for it. */
@Composable internal fun DeviceScreen(scene: Scene,vm: EditorViewModel,device: StoreDevice,modifier: Modifier) {
    val state by vm.state.collectAsState()
    val dark=state.theme==EditorTheme.Dark
    BoxWithConstraints(modifier,contentAlignment=if(device.landscape) Alignment.Center else Alignment.TopCenter) {
        val bezel=if(device.landscape) 10.dp else 8.dp
        val scale=minOf((maxWidth-bezel*2)/device.appWidth,if(device.landscape) (maxHeight-bezel*2)/device.appHeight else Float.MAX_VALUE)
        val shape=RoundedCornerShape(if(device.landscape) 14.dp else 36.dp)
        Box(Modifier.wrapContentHeight(Alignment.Top,unbounded=true).size(device.appWidth*scale+bezel*2,device.appHeight*scale+bezel*2).clip(shape).background(Color(0xFF0E0D0D))
            .border(1.dp,Color.White.copy(alpha=.12f),shape).padding(bezel)) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(if(device.landscape) 6.dp else 28.dp))) {
                Box(Modifier.wrapContentSize(Alignment.TopStart,unbounded=true).requiredSize(device.appWidth,device.appHeight)
                    .graphicsLayer(scaleX=scale,scaleY=scale,transformOrigin=TransformOrigin(0f,0f))) {
                    QrStudioTheme(dark) {
                        Column(Modifier.fillMaxSize()) {
                            StatusBar(dark)
                            Box(Modifier.weight(1f)) { EditorScreen(state,vm,dark) }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun StatusBar(dark: Boolean) {
    val bg=if(dark) Color(0xFF161514) else Paper;val fg=if(dark) Paper else Ink
    Row(Modifier.fillMaxWidth().height(32.dp).background(bg).padding(horizontal=22.dp),verticalAlignment=Alignment.CenterVertically) {
        Text("9:41",color=fg,fontSize=13.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically) {
            listOf(6.dp,9.dp,12.dp).forEach { Box(Modifier.size(4.dp,it).background(fg)) }
            Box(Modifier.size(20.dp,10.dp).border(1.5.dp,fg,RoundedCornerShape(3.dp)).padding(2.dp)) { Box(Modifier.fillMaxHeight().fillMaxWidth(.75f).background(fg)) }
        }
    }
}
