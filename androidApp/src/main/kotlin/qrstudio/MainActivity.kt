package qrstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalFocusManager
import qrstudio.platform.androidServices

class MainActivity: ComponentActivity() {
    private var snapshot: String?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val services=androidServices(this)
        snapshot=savedInstanceState?.getString("editor")
        setContent {
            val focus=LocalFocusManager.current
            BackHandler { focus.clearFocus() }
            App(services,initialJson=snapshot,onSnapshot={ snapshot=it })
        }
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("editor",snapshot);super.onSaveInstanceState(outState) }
}
