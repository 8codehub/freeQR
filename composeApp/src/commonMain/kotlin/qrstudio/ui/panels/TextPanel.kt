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
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable fun TextPanel(s: EditorState,vm: EditorViewModel,custom: (ColorTarget)->Unit) {
    val t=LocalTokens.current;val defaultText=stringResource(Res.string.default_layer_text)
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        s.texts.forEach { layer ->
            SecondaryButton(layer.text.lineSequence().firstOrNull().orEmpty().ifEmpty { stringResource(Res.string.empty) },modifier=Modifier.widthIn(max=160.dp).heightIn(min=40.dp),selected=s.selection==Selection.Text(layer.id),singleLine=true) { vm.select(Selection.Text(layer.id)) }
        }
        PrimaryButton(stringResource(Res.string.add_text),StudioIcon.Plus,modifier=Modifier.widthIn(max=160.dp)) { vm.addText(defaultText) }
    }
    if(s.texts.isEmpty()) Text(stringResource(Res.string.text_hint),color=t.muted,fontSize=13.sp)
    s.selectedText?.let { layer ->
        Field(stringResource(Res.string.text)) { TextInput(layer.text,{ vm.updateText { copy(text=it) } },minLines=2,minHeight=56,tag="layerText") }
        Field(stringResource(Res.string.font)) { SegmentedControl(listOf(stringResource(Res.string.font_sans),stringResource(Res.string.font_serif),stringResource(Res.string.font_mono)),layer.font.ordinal) { index -> vm.updateText { copy(font=TextFont.entries[index]) } } }
        Field(stringResource(Res.string.weight)) { SegmentedControl(listOf(stringResource(Res.string.weight_regular),stringResource(Res.string.weight_medium),stringResource(Res.string.weight_bold)),listOf(400,600,800).indexOf(layer.weight)) { index -> vm.updateText { copy(weight=listOf(400,600,800)[index]) } } }
        StudioSlider(stringResource(Res.string.size_px,(layer.size*1080).roundToInt()),layer.size,.025f.. .22f,steps=194) { size -> vm.updateText { copy(size=(size*1000).roundToInt()/1000f) } }
        Field(stringResource(Res.string.color)) { Swatches(layer.color,{ value -> vm.updateText { copy(color=value) } },{ custom(ColorTarget.Text) }) }
        SecondaryButton(stringResource(Res.string.delete_layer),StudioIcon.Trash2,Modifier.fillMaxWidth(),danger=true) { vm.deleteText() }
    }
}
