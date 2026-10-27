package qrstudio

import android.provider.MediaStore
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.Decoder
import com.google.zxing.DecodeHintType
import qrstudio.model.Ecl
import qrstudio.qr.QrEncoder

class EditorUiTest {
    @get:Rule val editor=createAndroidComposeRule<MainActivity>()
    @Test fun wifiCanBeEnteredAndSavedToMediaStore() {
        editor.onNodeWithText("Content",useUnmergedTree=true).performClick()
        editor.onNodeWithText("Wi-Fi").performClick()
        editor.onNodeWithTag("ssid").performTextReplacement("QR Studio UI Test")
        editor.onNodeWithTag("wifiPassword").performTextReplacement("test;password")
        editor.activityRule.scenario.onActivity { it.currentFocus?.clearFocus() }
        editor.onNodeWithContentDescription("QR design canvas").performClick()
        editor.waitForIdle()
        editor.onNodeWithText("Save").performClick()
        val resolver=InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val started=System.currentTimeMillis()/1000-2
        var saved: android.net.Uri?=null
        editor.waitUntil(20000) {
            resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,arrayOf(MediaStore.Images.Media._ID),"${MediaStore.Images.Media.DISPLAY_NAME} LIKE ? AND ${MediaStore.Images.Media.DATE_ADDED} >= ?",arrayOf("qr-studio-%",started.toString()),"${MediaStore.Images.Media._ID} DESC")?.use { cursor ->
                if(cursor.moveToFirst()) { saved=android.content.ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cursor.getLong(0));true } else false
            } ?: false
        }
        assertNotNull(saved)
        saved?.let { resolver.delete(it,null,null) }
    }
    @Test fun pureCommonEncoderDecodesForEveryEclIncludingUnicode() {
        Ecl.entries.forEach { ecl ->
            val payload="QR Studio café 世界 مرحبا"
            val matrix=QrEncoder().encode(payload,ecl)!!
            val bits=BitMatrix(matrix.n)
            for(r in 0 until matrix.n) for(c in 0 until matrix.n) if(matrix.isDark(r,c)) bits.set(c,r)
            assertEquals(payload,Decoder().decode(bits,mapOf(DecodeHintType.CHARACTER_SET to "UTF-8")).text)
        }
    }
    @Test fun keyboardEditingKeepsTheFieldVisibleAndRestoresNavigationOnDone() {
        editor.onNodeWithText("Content",useUnmergedTree=true).performClick()
        editor.onNodeWithText("URL").performClick()
        editor.onNodeWithTag("data").performClick()
        editor.waitUntil(15000) { editor.onAllNodesWithTag("keyboard-done").fetchSemanticsNodes().isNotEmpty() }
        editor.onNodeWithTag("data").assertIsDisplayed()
        editor.onNodeWithTag("bottom-navigation").assertDoesNotExist()
        editor.onNodeWithTag("keyboard-done").performClick()
        editor.waitUntil(15000) { editor.onAllNodesWithTag("bottom-navigation").fetchSemanticsNodes().isNotEmpty() }
        editor.onNodeWithTag("bottom-navigation").assertIsDisplayed()
    }

}
