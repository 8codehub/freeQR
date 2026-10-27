package qrstudio.ui.panels

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
    Field("Canvas") { SegmentedControl(listOf("1:1","4:5","Story","16:9"),s.ratio.ordinal) { vm.edit { copy(ratio=CanvasRatio.entries[it]) } } }
    Field("Fill") { SegmentedControl(listOf("Transparent","Color","Image"),s.bgMode.ordinal) { vm.edit { copy(bgMode=BackgroundMode.entries[it]) } } }
    when(s.bgMode) {
        BackgroundMode.Solid -> Field("Color") { Swatches(s.bg,{ value -> vm.edit { copy(bg=value) } },{ custom(ColorTarget.Background) }) }
        BackgroundMode.Image -> {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                SecondaryButton(if(s.bgImage==null) "Choose image" else "Replace image",StudioIcon.Image,Modifier.weight(1f)) { vm.chooseImage() }
                if(s.bgImage!=null) SecondaryButton("Remove") { vm.removeImage() }
            }
            Text("The image fills the canvas. Add a gradient to keep the code readable.",color=t.muted,fontSize=12.sp)
        }
        BackgroundMode.Transparent -> Text("Saved as PNG with a transparent background. Turn on the backing plate if the code sits on dark surfaces.",color=t.muted,fontSize=12.sp)
    }
}
