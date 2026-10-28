package qrstudio.ui.panels

import org.jetbrains.compose.resources.stringResource
import qrstudio.generated.resources.*
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
    Field(stringResource(Res.string.type)) { SegmentedControl(listOf(stringResource(Res.string.type_url),stringResource(Res.string.text),stringResource(Res.string.type_wifi),stringResource(Res.string.type_phone)),s.type.ordinal) { vm.edit { copy(type=ContentType.entries[it]) } } }
    when(s.type) {
        ContentType.Url,ContentType.Text -> Field(if(s.type==ContentType.Url) stringResource(Res.string.link) else stringResource(Res.string.text)) { TextInput(s.data,{ value -> vm.edit { copy(data=value) } },"https://",minLines=3,minHeight=72,keyboard=if(s.type==ContentType.Url) KeyboardType.Uri else KeyboardType.Text,tag="data") }
        ContentType.Wifi -> {
            Field(stringResource(Res.string.ssid)) { TextInput(s.ssid,{ value -> vm.edit { copy(ssid=value) } },tag="ssid") }
            Field(stringResource(Res.string.password)) { TextInput(s.wifiPass,{ value -> vm.edit { copy(wifiPass=value) } },keyboard=KeyboardType.Password,tag="wifiPassword") }
        }
        ContentType.Phone -> Field(stringResource(Res.string.phone_number)) { TextInput(s.phone,{ value -> vm.edit { copy(phone=value) } },keyboard=KeyboardType.Phone,tag="phone") }
    }
    Field(stringResource(Res.string.error_correction)) { SegmentedControl(listOf(stringResource(Res.string.ecl_low),stringResource(Res.string.ecl_medium),stringResource(Res.string.ecl_high),stringResource(Res.string.ecl_max)),s.ecl.ordinal) { vm.edit { copy(ecl=Ecl.entries[it]) } } }
    if(s.encodingFailed) Text(stringResource(Res.string.too_much_data),color=t.accent,fontSize=12.sp)
    else s.matrix?.let { Text(stringResource(Res.string.modules_info,it.n),color=t.muted,fontSize=12.sp) }
}
