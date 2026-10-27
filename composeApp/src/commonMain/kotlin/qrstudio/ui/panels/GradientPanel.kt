package qrstudio.ui.panels

import androidx.compose.runtime.Composable
import qrstudio.model.*
import qrstudio.ui.components.*
import qrstudio.vm.EditorViewModel
import kotlin.math.roundToInt

@Composable fun GradientPanel(s: EditorState,vm: EditorViewModel,custom: (ColorTarget)->Unit) {
    Field("Gradient layer") { SegmentedControl(listOf("Off","On"),if(s.grad) 1 else 0) { vm.edit { copy(grad=it==1) } } }
    Field("Direction") { SegmentedControl(listOf("Top → down","Bottom → up"),s.gradDir.ordinal) { vm.edit { copy(grad=true,gradDir=GradientDirection.entries[it]) } } }
    Field("Color") { Swatches(s.gradColor,{ value -> vm.edit { copy(grad=true,gradColor=value) } },{ custom(ColorTarget.Gradient) }) }
    StudioSlider("Strength · ${(s.gradOpacity*100).roundToInt()}%",s.gradOpacity,0f..1f) { value -> vm.edit { copy(grad=true,gradOpacity=value) } }
    StudioSlider("Length · ${(s.gradLen*100).roundToInt()}%",s.gradLen,.1f..1f) { value -> vm.edit { copy(grad=true,gradLen=value) } }
}
