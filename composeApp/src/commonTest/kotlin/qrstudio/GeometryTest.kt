package qrstudio

import kotlin.test.*
import qrstudio.model.*
import qrstudio.render.*
class GeometryTest {
    @Test fun snapUsesTwoPercentThresholdAndAlwaysClamps() {
        assertEquals(SnapResult(.5f,.5f,true,true),snapCenter(.49f,.51f,true))
        assertEquals(SnapResult(.6f,.4f,false,false),snapCenter(.6f,.4f,true))
        assertEquals(SnapResult(0f,1f,false,false),snapCenter(-1f,2f,false))
    }
    @Test fun topmostTextWinsThenQrThenNothing() {
        val hits=listOf(LayerHit(Selection.Qr,HitBox(0f,0f,100f,100f)),LayerHit(Selection.Text(1),HitBox(20f,20f,60f,60f)),LayerHit(Selection.Text(2),HitBox(30f,30f,70f,70f)))
        assertEquals(Selection.Text(2),hitTest(40f,40f,hits));assertEquals(Selection.Text(1),hitTest(25f,25f,hits));assertEquals(Selection.Qr,hitTest(10f,10f,hits));assertEquals(Selection.None,hitTest(200f,200f,hits))
    }
    @Test fun upAndDownGradientGeometryAreMirrors() {
        assertEquals(GradientGeometry(0f,600f,0f,600f),gradientGeometry(1000f,.6f,false))
        assertEquals(GradientGeometry(1000f,400f,400f,600f),gradientGeometry(1000f,.6f,true))
    }
}
