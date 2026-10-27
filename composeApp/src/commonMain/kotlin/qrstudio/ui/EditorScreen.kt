package qrstudio.ui

import org.jetbrains.compose.resources.stringResource
import qrstudio.generated.resources.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import qrstudio.ui.components.SecondaryButton
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import qrstudio.model.*
import qrstudio.render.exportPng
import qrstudio.theme.*
import qrstudio.ui.components.ColorPickerSheet
import qrstudio.ui.panels.*
import qrstudio.vm.EditorViewModel

@Composable fun EditorScreen(state: EditorState,vm: EditorViewModel,dark: Boolean) {
    val t=LocalTokens.current;val focus=LocalFocusManager.current;val sans=archivoFamily()
    val keyboard=LocalSoftwareKeyboardController.current
    val imeVisible=WindowInsets.ime.getBottom(LocalDensity.current)>0
    val measurer=rememberTextMeasurer();val exportMeasurer=rememberTextMeasurer(cacheSize=0)
    val scroll=rememberScrollState();var picker by remember { mutableStateOf<ColorTarget?>(null) }
    LaunchedEffect(state.tab) { scroll.scrollTo(0) }
    BoxWithConstraints(Modifier.fillMaxSize().background(t.bg).windowInsetsPadding(WindowInsets.statusBars).imePadding()) {
        val fontScale=LocalDensity.current.fontScale
        val targetPreview=when {
            imeVisible -> minOf(112.dp,maxHeight*.22f).coerceAtLeast(72.dp)
            maxHeight<520.dp || fontScale>1.5f -> minOf(330.dp,maxHeight*.32f)
            else -> 330.dp
        }
        val previewHeight by animateDpAsState(targetPreview,tween(220),label="keyboard-preview")
        Column(Modifier.fillMaxSize()) {
            TopBar(dark,state.exporting,onTheme={ vm.edit { copy(theme=if(dark) EditorTheme.Light else EditorTheme.Dark) } },
                onShare={ focus.clearFocus();vm.export(true) { exportPng(it,exportMeasurer,sans) } },
                onSave={ focus.clearFocus();vm.export(false) { exportPng(it,exportMeasurer,sans) } })
            PreviewCanvas(state,vm,measurer,sans,previewHeight)
            if(!imeVisible) HintBar(state)
            else Row(Modifier.fillMaxWidth().heightIn(min=48.dp).padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
                Text(stringResource(if(state.tab==EditorTab.Text) Res.string.editing_text else Res.string.editing_content),Modifier.weight(1f),color=t.muted,fontSize=12.sp)
                SecondaryButton(stringResource(Res.string.keyboard_done),modifier=Modifier.testTag("keyboard-done")) { keyboard?.hide();focus.clearFocus() }
            }
            Box(Modifier.weight(1f).fillMaxWidth().testTag("editor-tools")) {
                Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    when(state.tab) {
                        EditorTab.Content -> ContentPanel(state,vm)
                        EditorTab.QrStyle -> QrStylePanel(state,vm) { picker=it }
                        EditorTab.Background -> BackgroundPanel(state,vm) { picker=it }
                        EditorTab.Gradient -> GradientPanel(state,vm) { picker=it }
                        EditorTab.Text -> TextPanel(state,vm) { picker=it }
                    }
                }
                state.snack?.let { EditorSnackbar(it,Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom=12.dp)) }
            }
            if(!imeVisible) BottomNav(state.tab) { focus.clearFocus();vm.tab(it) }
        }
    }
    picker?.let { target ->
        val color=when(target) { ColorTarget.Module -> state.fg;ColorTarget.Eye -> state.eye;ColorTarget.Plate -> state.plateColor;ColorTarget.Background -> state.bg;ColorTarget.Gradient -> state.gradColor;ColorTarget.Text -> state.selectedText?.color ?: 0xFF201E1D }
        ColorPickerSheet(color,onChange={ value -> when(target) {
            ColorTarget.Module -> vm.edit { copy(fg=value) };ColorTarget.Eye -> vm.edit { copy(eye=value) }
            ColorTarget.Plate -> vm.edit { copy(plateColor=value) };ColorTarget.Background -> vm.edit { copy(bg=value) }
            ColorTarget.Gradient -> vm.edit { copy(grad=true,gradColor=value) };ColorTarget.Text -> vm.updateText { copy(color=value) }
        } },onDismiss={ picker=null })
    }
}
