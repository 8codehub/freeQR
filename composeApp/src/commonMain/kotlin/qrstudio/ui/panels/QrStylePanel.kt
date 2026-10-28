package qrstudio.ui.panels

import org.jetbrains.compose.resources.stringResource
import qrstudio.generated.resources.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import qrstudio.model.*
import qrstudio.ui.components.*
import qrstudio.vm.EditorViewModel
import kotlin.math.roundToInt

@Composable fun QrStylePanel(s: EditorState,vm: EditorViewModel,custom: (ColorTarget)->Unit) {
    Field(stringResource(Res.string.module_shape)) { SegmentedControl(listOf(stringResource(Res.string.square),stringResource(Res.string.fluid),stringResource(Res.string.dots),stringResource(Res.string.diamond),stringResource(Res.string.lines)),s.modStyle.ordinal) { vm.edit { copy(modStyle=ModuleStyle.entries[it]) } } }
    Field(stringResource(Res.string.corner_style)) { SegmentedControl(listOf(stringResource(Res.string.square),stringResource(Res.string.rounded),stringResource(Res.string.circle),stringResource(Res.string.leaf)),s.eyeStyle.ordinal) { vm.edit { copy(eyeStyle=EyeStyle.entries[it]) } } }
    Field(stringResource(Res.string.module_color)) { Swatches(s.fg,{ value -> vm.edit { copy(fg=value) } },{ custom(ColorTarget.Module) }) }
    Field(stringResource(Res.string.corner_color)) { Swatches(s.eye,{ value -> vm.edit { copy(eye=value) } },{ custom(ColorTarget.Eye) }) }
    Field(stringResource(Res.string.backing_plate)) { SegmentedControl(listOf(stringResource(Res.string.none),stringResource(Res.string.plate)),if(s.plate) 1 else 0) { vm.edit { copy(plate=it==1) } } }
    if(s.plate) Swatches(s.plateColor,{ value -> vm.edit { copy(plateColor=value) } },{ custom(ColorTarget.Plate) })
    StudioSlider(stringResource(Res.string.size_percent,(s.qrSize*100).roundToInt()),s.qrSize,.15f.. .95f,steps=79) { value -> vm.edit { copy(qrSize=(value*100).roundToInt()/100f) } }
    SecondaryButton(stringResource(Res.string.center_qr),modifier=Modifier.fillMaxWidth()) { vm.edit { copy(qx=.5f,qy=.5f,selection=Selection.Qr) } }
}
