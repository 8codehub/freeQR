package com.qr.domain

import org.junit.Assert.*
import org.junit.Test

class QrPayloadTest {
    @Test fun websiteAcceptsOnlyCompleteHttpLinks() {
        listOf("example.com", "javascript:alert(1)", "https://", "https://a b.com").forEach { value ->
            assertThrows(InvalidContent::class.java) { QrPayload.encode(QrDraft(fields = mapOf("url" to value))) }
        }
        assertEquals("https://example.com/path?q=1", QrPayload.encode(QrDraft(fields = mapOf("url" to "https://example.com/path?q=1"))))
    }
    @Test fun wifiEscapesReservedCharactersAndPreservesPasswordWhitespace() {
        val draft = QrDraft(type = ContentType.WIFI, fields = mapOf("ssid" to "Cafe;Guest", "password" to " pa:ss\\word "), hiddenWifi = true)
        assertEquals("WIFI:T:WPA;S:Cafe\\;Guest;P: pa\\:ss\\\\word ;H:true;;", QrPayload.encode(draft))
    }
    @Test fun openWifiNeverLeaksTheOldPassword() {
        val draft = QrDraft(type = ContentType.WIFI, fields = mapOf("ssid" to "Guest", "password" to "secret"), openWifi = true)
        assertEquals("WIFI:T:nopass;S:Guest;P:;H:false;;", QrPayload.encode(draft))
    }
    @Test fun emailEncodesQueryDelimitersAndUnicode() {
        val payload = QrPayload.encode(QrDraft(type = ContentType.EMAIL, fields = mapOf("email" to "hi@example.com", "subject" to "Hello & welcome", "body" to "Café")))
        assertEquals("mailto:hi@example.com?subject=Hello%20%26%20welcome&body=Caf%C3%A9", payload)
    }
    @Test fun contactCannotInjectAnotherVcardField() {
        val payload = QrPayload.encode(QrDraft(type = ContentType.CONTACT, fields = mapOf("name" to "Alex\r\nURL:evil", "organization" to "A;B,C")))
        assertTrue(payload.contains("FN:Alex\\nURL:evil\r\n"))
        assertTrue(payload.contains("ORG:A\\;B\\,C\r\n"))
    }
    @Test fun phoneNormalizesFormattingAndRejectsUriInjection() {
        assertEquals("tel:+15550102020", QrPayload.encode(QrDraft(type = ContentType.PHONE, fields = mapOf("phone" to "+1 (555) 010-2020"))))
        assertThrows(InvalidContent::class.java) { QrPayload.encode(QrDraft(type = ContentType.PHONE, fields = mapOf("phone" to "123?body=evil"))) }
    }
    @Test fun textPreservesUserWhitespace() {
        assertEquals("  hi\nthere  ", QrPayload.encode(QrDraft(type = ContentType.TEXT, fields = mapOf("text" to "  hi\nthere  "))))
    }
    @Test fun emptyAndOverlongContentAreRejected() {
        assertThrows(InvalidContent::class.java) { QrPayload.encode(QrDraft(type = ContentType.TEXT)) }
        assertThrows(InvalidContent::class.java) { QrPayload.encode(QrDraft(type = ContentType.TEXT, fields = mapOf("text" to "a".repeat(2001)))) }
    }
}
