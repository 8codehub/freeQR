package qrstudio.ui.panels

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import qrstudio.model.*
import qrstudio.ui.components.*
import qrstudio.vm.EditorViewModel
import kotlin.math.roundToInt

@Composable fun QrStylePanel(s: EditorState,vm: EditorViewModel,custom: (ColorTarget)->Unit) {
    Field("Module shape") { SegmentedControl(listOf("Square","Fluid","Dots","Diamond","Lines"),s.modStyle.ordinal) { vm.edit { copy(modStyle=ModuleStyle.entries[it]) } } }
    Field("Corner style") { SegmentedControl(listOf("Square","Rounded","Circle","Leaf"),s.eyeStyle.ordinal) { vm.edit { copy(eyeStyle=EyeStyle.entries[it]) } } }
    Field("Module color") { Swatches(s.fg,{ value -> vm.edit { copy(fg=value) } },{ custom(ColorTarget.Module) }) }
    Field("Corner color") { Swatches(s.eye,{ value -> vm.edit { copy(eye=value) } },{ custom(ColorTarget.Eye) }) }
    Field("Backing plate") { SegmentedControl(listOf("None","Plate"),if(s.plate) 1 else 0) { vm.edit { copy(plate=it==1) } } }
    if(s.plate) Swatches(s.plateColor,{ value -> vm.edit { copy(plateColor=value) } },{ custom(ColorTarget.Plate) })
    StudioSlider("Size · ${(s.qrSize*100).roundToInt()}%",s.qrSize,.15f.. .95f,steps=79) { value -> vm.edit { copy(qrSize=(value*100).roundToInt()/100f) } }
    SecondaryButton("Center QR code",modifier=Modifier.fillMaxWidth()) { vm.edit { copy(qx=.5f,qy=.5f,selection=Selection.Qr) } }
}
