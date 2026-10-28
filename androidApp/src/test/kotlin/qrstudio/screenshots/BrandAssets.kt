package qrstudio.screenshots

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import qrstudio.platform.PlatformServices
import qrstudio.theme.archivoFamily
import qrstudio.vm.EditorViewModel
import java.io.File

// Play listing app icon (512×512) and feature graphic (1024×500); mdpi so 1dp is 1px.
// ./gradlew :androidApp:testDebugUnitTest --tests 'qrstudio.screenshots.BrandAssets'
@RunWith(RobolectricTestRunner::class) @GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=[34],qualifiers="w1100dp-h1500dp-port-mdpi")
class BrandAssets {
    @get:Rule val compose=createComposeRule()
    private val out=File(System.getProperty("storeScreenshotsDir") ?: "build/play-store-screenshots").apply { mkdirs() }

    @Test fun render() {
        val vm=EditorViewModel(PlatformServices(NoServices,NoServices,NoServices,NoServices,NoServices))
        val scene=scenes.first { it.file=="04-photo" }
        vm.edit { scene.state.copy(bgImage=samplePhoto()) }
        compose.setContent {
            Column {
                Box(Modifier.size(512.dp).testTag("icon")) { AppIcon(Modifier.fillMaxSize()) }
                FeatureGraphic(scene,vm)
            }
        }
        repeat(200) { if(vm.state.value.matrix==null) { ShadowLooper.idleMainLooper();Thread.sleep(25) } }
        check(vm.state.value.matrix!=null)
        compose.waitForIdle()
        save("icon","app-icon-512.png")
        // Play rejects feature graphics with an alpha channel
        save("feature","feature-graphic-1024x500.jpg",Bitmap.CompressFormat.JPEG)
    }
    private fun save(tag: String,name: String,format: Bitmap.CompressFormat=Bitmap.CompressFormat.PNG)=File(out,name).outputStream().use {
        compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap().compress(format,if(format==Bitmap.CompressFormat.JPEG) 95 else 100,it)
    }
}

private val AccentDeep=Color(0xFFC52409)

/** Full-bleed square: Play applies its own corner mask and shadow, so neither is drawn here. */
@Composable fun AppIcon(modifier: Modifier) {
    Box(modifier.background(Brush.linearGradient(listOf(Color(0xFFFF563C),Accent,AccentDeep),start=Offset.Zero,end=Offset.Infinite))) {
        IconGlyph(Modifier.fillMaxSize(),inset=.2f)
    }
}

/**
 * Three rounded finder eyes like the editor's "Rounded" corner style, with the fourth corner
 * made of dots and one ink module — the app's own module styles, reduced to a mark.
 */
@Composable fun IconGlyph(modifier: Modifier,inset: Float) {
    Canvas(modifier) {
        val grid=17;val side=size.minDimension*(1-inset*2);val m=side/grid
        val origin=Offset((size.width-side)/2,(size.height-side)/2)
        fun cell(c: Float,r: Float)=origin+Offset(c*m,r*m)
        eye(cell(0f,0f),m);eye(cell(10f,0f),m);eye(cell(0f,10f),m)
        // Data modules between and beside the eyes, drawn as fluid dots
        listOf(8 to 1,8 to 3,8 to 5,1 to 8,3 to 8,5 to 8,8 to 8,10 to 8,12 to 8,14 to 8,16 to 8,8 to 10,8 to 12,8 to 14,8 to 16,
            10 to 10,12 to 10,16 to 10,11 to 12,13 to 12,15 to 12,10 to 14,12 to 14,16 to 14,11 to 16,13 to 16,16 to 16).forEach { (c,r) ->
            drawCircle(Color.White,m*.46f,cell(c+.5f,r+.5f))
        }
        drawRoundRect(Ink,cell(14f,13.5f).let { Offset(it.x-m*.05f,it.y) },Size(m*2.1f,m*2.1f),CornerRadius(m*.7f))
    }
}

// Mirrors res/drawable/ic_launcher_foreground.xml: the ring is cut out so the background shows through
private fun DrawScope.eye(at: Offset,m: Float) {
    val ring=Path().apply {
        fillType=PathFillType.EvenOdd
        addRoundRect(RoundRect(Rect(at,Size(m*7,m*7)),CornerRadius(m*2.2f)))
        addRoundRect(RoundRect(Rect(at+Offset(m,m),Size(m*5,m*5)),CornerRadius(m*1.4f)))
    }
    drawPath(ring,Color.White)
    drawRoundRect(Color.White,at+Offset(m*2,m*2),Size(m*3,m*3),CornerRadius(m*.9f))
}

@Composable private fun FeatureGraphic(scene: Scene,vm: EditorViewModel) {
    val font=archivoFamily()
    Box(Modifier.size(1024.dp,500.dp).clipToBounds().background(Brush.linearGradient(listOf(Color(0xFFFF4A2E),Accent,AccentDeep),start=Offset.Zero,end=Offset(1024f,500f))).testTag("feature")) {
        // Oversized faint mark for depth behind the phone
        IconGlyph(Modifier.size(620.dp).offset(x=560.dp,y=(-60).dp),inset=0f)
        Box(Modifier.matchParentSize().background(Accent.copy(alpha=.86f)))
        Column(Modifier.padding(start=64.dp).width(520.dp).fillMaxHeight(),verticalArrangement=Arrangement.Center) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                Box(Modifier.size(72.dp).background(Color.White,RoundedCornerShape(18.dp)).padding(6.dp)) {
                    Box(Modifier.fillMaxSize().background(Accent,RoundedCornerShape(14.dp))) { IconGlyph(Modifier.fillMaxSize(),inset=.16f) }
                }
                Text("QR Studio",color=Color.White,fontFamily=font,fontWeight=FontWeight.ExtraBold,fontSize=44.sp)
            }
            Spacer(Modifier.height(28.dp))
            Text("Design QR codes\nthat stand out",color=Color.White,fontFamily=font,fontWeight=FontWeight.ExtraBold,fontSize=48.sp,lineHeight=52.sp)
            Spacer(Modifier.height(16.dp))
            Text("Links, Wi-Fi, text and phone · shapes, colors, photos",color=Color.White.copy(alpha=.85f),fontFamily=font,fontSize=19.sp)
        }
        DeviceScreen(scene,vm,StoreDevice("feature",0.dp,0.dp,392.dp,820.dp),Modifier.align(Alignment.TopEnd).padding(top=44.dp,end=72.dp).size(272.dp,520.dp))
    }
}
