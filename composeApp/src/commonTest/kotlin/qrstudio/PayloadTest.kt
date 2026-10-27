package qrstudio

import kotlin.test.*
import qrstudio.model.*
import qrstudio.qr.*

class PayloadTest {
    @Test fun urlsAndTextArePreservedAndEmptyContentEncodesOneSpace() {
        assertEquals("https://example.com",buildPayload(EditorState()))
        assertEquals(" ",buildPayload(EditorState(data="")))
        assertEquals("  café 世界\n",buildPayload(EditorState(type=ContentType.Text,data="  café 世界\n")))
    }
    @Test fun wifiEscapesEveryReservedCharacter() {
        val ssid="Cafe;Guest,\"Room:1\\2"
        val pass=" p;a,s:s\"\\ "
        assertEquals("WIFI:T:WPA;S:${escapeWifi(ssid)};P:${escapeWifi(pass)};;",buildPayload(EditorState(type=ContentType.Wifi,ssid=ssid,wifiPass=pass)))
        assertEquals("Cafe\\;Guest\\,\\\"Room\\:1\\\\2",escapeWifi(ssid))
        assertEquals("WIFI:T:nopass;S:Guest;P:;;",buildPayload(EditorState(type=ContentType.Wifi,ssid="Guest")))
    }
    @Test fun phonePayloadUsesTheExactNumber() { assertEquals("tel:+1 555",buildPayload(EditorState(type=ContentType.Phone,phone="+1 555"))) }
}
