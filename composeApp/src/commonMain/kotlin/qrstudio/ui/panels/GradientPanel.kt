package qrstudio.ui.panels

import org.jetbrains.compose.resources.stringResource
import qrstudio.generated.resources.*
import androidx.compose.runtime.Composable
import qrstudio.model.*
import qrstudio.ui.components.*
import qrstudio.vm.EditorViewModel
import kotlin.math.roundToInt

@Composable fun GradientPanel(s: EditorState,vm: EditorViewModel,custom: (ColorTarget)->Unit) {
    Field(stringResource(Res.string.gradient_layer)) { SegmentedControl(listOf(stringResource(Res.string.off),stringResource(Res.string.on)),if(s.grad) 1 else 0) { vm.edit { copy(grad=it==1) } } }
    Field(stringResource(Res.string.direction)) { SegmentedControl(listOf(stringResource(Res.string.top_down),stringResource(Res.string.bottom_up)),s.gradDir.ordinal) { vm.edit { copy(grad=true,gradDir=GradientDirection.entries[it]) } } }
    Field(stringResource(Res.string.color)) { Swatches(s.gradColor,{ value -> vm.edit { copy(grad=true,gradColor=value) } },{ custom(ColorTarget.Gradient) }) }
    StudioSlider(stringResource(Res.string.strength,(s.gradOpacity*100).roundToInt()),s.gradOpacity,0f..1f) { value -> vm.edit { copy(grad=true,gradOpacity=value) } }
    StudioSlider(stringResource(Res.string.length,(s.gradLen*100).roundToInt()),s.gradLen,.1f..1f) { value -> vm.edit { copy(grad=true,gradLen=value) } }
}
