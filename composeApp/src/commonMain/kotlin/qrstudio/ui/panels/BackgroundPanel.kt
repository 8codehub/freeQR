package qrstudio.ui.panels

import org.jetbrains.compose.resources.stringResource
import qrstudio.generated.resources.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import qrstudio.model.*
import qrstudio.ui.components.*
import qrstudio.theme.LocalTokens
import qrstudio.vm.EditorViewModel

@Composable fun BackgroundPanel(s: EditorState,vm: EditorViewModel,custom: (ColorTarget)->Unit) {
    val t=LocalTokens.current
    Field(stringResource(Res.string.canvas)) { SegmentedControl(listOf("1:1","4:5",stringResource(Res.string.ratio_story),"16:9"),s.ratio.ordinal) { vm.edit { copy(ratio=CanvasRatio.entries[it]) } } }
    Field(stringResource(Res.string.fill)) { SegmentedControl(listOf(stringResource(Res.string.transparent),stringResource(Res.string.color),stringResource(Res.string.image)),s.bgMode.ordinal) { vm.edit { copy(bgMode=BackgroundMode.entries[it]) } } }
    when(s.bgMode) {
        BackgroundMode.Solid -> Field(stringResource(Res.string.color)) { Swatches(s.bg,{ value -> vm.edit { copy(bg=value) } },{ custom(ColorTarget.Background) }) }
        BackgroundMode.Image -> {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                SecondaryButton(if(s.bgImage==null) stringResource(Res.string.choose_image) else stringResource(Res.string.replace_image),StudioIcon.Image,Modifier.weight(1f)) { vm.chooseImage() }
                if(s.bgImage!=null) SecondaryButton(stringResource(Res.string.remove)) { vm.removeImage() }
            }
            Text(stringResource(Res.string.image_hint),color=t.muted,fontSize=12.sp)
        }
        BackgroundMode.Transparent -> Text(stringResource(Res.string.transparent_hint),color=t.muted,fontSize=12.sp)
    }
}
