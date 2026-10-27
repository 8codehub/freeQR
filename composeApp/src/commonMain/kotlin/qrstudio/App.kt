package qrstudio

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import qrstudio.model.EditorTheme
import qrstudio.platform.PlatformServices
import qrstudio.theme.QrStudioTheme
import qrstudio.ui.EditorScreen
import qrstudio.vm.EditorViewModel

@Composable fun App(services: PlatformServices,initialJson: String?=null,onSnapshot: (String)->Unit={}) {
    val vm=viewModel { EditorViewModel(services,initialJson) }
    val state by vm.state.collectAsStateWithLifecycle()
    val dark=when(state.theme) { EditorTheme.System -> isSystemInDarkTheme();EditorTheme.Light -> false;EditorTheme.Dark -> true }
    SideEffect { services.themeChanged(dark);onSnapshot(vm.snapshot()) }
    QrStudioTheme(dark) { EditorScreen(state,vm,dark) }
}
