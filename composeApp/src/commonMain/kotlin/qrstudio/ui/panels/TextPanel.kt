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
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable fun TextPanel(s: EditorState,vm: EditorViewModel,custom: (ColorTarget)->Unit) {
    val t=LocalTokens.current
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        s.texts.forEach { layer ->
            SecondaryButton(layer.text.lineSequence().firstOrNull().orEmpty().ifEmpty { "Empty" },modifier=Modifier.widthIn(max=160.dp).heightIn(min=40.dp),selected=s.selection==Selection.Text(layer.id),singleLine=true) { vm.select(Selection.Text(layer.id)) }
        }
        PrimaryButton("Add text",StudioIcon.Plus,modifier=Modifier.widthIn(max=160.dp)) { vm.addText() }
    }
    if(s.texts.isEmpty()) Text("Add a text layer, then drag it anywhere on the canvas.",color=t.muted,fontSize=13.sp)
    s.selectedText?.let { layer ->
        Field("Text") { TextInput(layer.text,{ vm.updateText { copy(text=it) } },minLines=2,minHeight=56,tag="layerText") }
        Field("Font") { SegmentedControl(listOf("Sans","Serif","Mono"),layer.font.ordinal) { index -> vm.updateText { copy(font=TextFont.entries[index]) } } }
        Field("Weight") { SegmentedControl(listOf("Regular","Medium","Bold"),listOf(400,600,800).indexOf(layer.weight)) { index -> vm.updateText { copy(weight=listOf(400,600,800)[index]) } } }
        StudioSlider("Size · ${(layer.size*1080).roundToInt()} px",layer.size,.025f.. .22f,steps=194) { size -> vm.updateText { copy(size=(size*1000).roundToInt()/1000f) } }
        Field("Color") { Swatches(layer.color,{ value -> vm.updateText { copy(color=value) } },{ custom(ColorTarget.Text) }) }
        SecondaryButton("Delete layer",StudioIcon.Trash2,Modifier.fillMaxWidth(),danger=true) { vm.deleteText() }
    }
}
