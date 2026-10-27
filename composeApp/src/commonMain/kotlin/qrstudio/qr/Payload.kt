package qrstudio.qr

import qrstudio.model.ContentType
import qrstudio.model.EditorState
fun buildPayload(state: EditorState): String = when(state.type) {
    ContentType.Url, ContentType.Text -> state.data.ifEmpty { " " }
    ContentType.Wifi -> "WIFI:T:${if(state.wifiPass.isEmpty()) "nopass" else "WPA"};S:${escapeWifi(state.ssid)};P:${escapeWifi(state.wifiPass)};;"
    ContentType.Phone -> "tel:${state.phone}"
}
fun escapeWifi(value: String): String = buildString {
    value.forEach { if(it in "\\;,:\"") append('\\');append(it) }
}
