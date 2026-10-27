package qrstudio

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import qrstudio.platform.iosServices

fun MainViewController(): UIViewController {
    val services=iosServices()
    return ComposeUIViewController { App(services) }
}
