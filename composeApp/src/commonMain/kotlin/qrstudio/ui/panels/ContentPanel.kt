package qrstudio.ui.panels

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import qrstudio.model.*
import qrstudio.ui.components.*
import qrstudio.theme.LocalTokens
import qrstudio.vm.EditorViewModel

@Composable fun ContentPanel(s: EditorState,vm: EditorViewModel) {
    val t=LocalTokens.current
    Field("Type") { SegmentedControl(listOf("URL","Text","Wi-Fi","Phone"),s.type.ordinal) { vm.edit { copy(type=ContentType.entries[it]) } } }
    when(s.type) {
        ContentType.Url,ContentType.Text -> Field(if(s.type==ContentType.Url) "Link" else "Text") { TextInput(s.data,{ value -> vm.edit { copy(data=value) } },"https://",minLines=3,minHeight=72,keyboard=if(s.type==ContentType.Url) KeyboardType.Uri else KeyboardType.Text,tag="data") }
        ContentType.Wifi -> {
            Field("Network name (SSID)") { TextInput(s.ssid,{ value -> vm.edit { copy(ssid=value) } },tag="ssid") }
            Field("Password") { TextInput(s.wifiPass,{ value -> vm.edit { copy(wifiPass=value) } },keyboard=KeyboardType.Password,tag="wifiPassword") }
        }
        ContentType.Phone -> Field("Phone number") { TextInput(s.phone,{ value -> vm.edit { copy(phone=value) } },keyboard=KeyboardType.Phone,tag="phone") }
    }
    Field("Error correction") { SegmentedControl(listOf("Low","Medium","High","Max"),s.ecl.ordinal) { vm.edit { copy(ecl=Ecl.entries[it]) } } }
    if(s.encodingFailed) Text("Too much data for one QR code. Shorten the content or lower error correction.",color=t.accent,fontSize=12.sp)
    else s.matrix?.let { Text("${it.n} × ${it.n} modules. Higher correction survives more overlap from text and images.",color=t.muted,fontSize=12.sp) }
}
