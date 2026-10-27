package qrstudio

import kotlin.test.*
import kotlinx.serialization.json.Json
import qrstudio.model.*
class LayersTest {
    @Test fun addedLayersAlternatePositionsHaveUniqueIdsAndSelectTextTab() {
        val first=EditorState().withAddedText(100)
        assertEquals(.12f,first.texts.single().y);assertEquals(.07f,first.texts.single().size)
        assertEquals("Scan me",first.texts.single().text);assertEquals(800,first.texts.single().weight)
        val second=first.withAddedText(100)
        assertEquals(.88f,second.texts.last().y);assertTrue(second.texts.last().id>first.texts.single().id)
        assertEquals(EditorTab.Text,second.tab);assertEquals(Selection.Text(second.texts.last().id),second.selection)
    }
    @Test fun imageAndDarkBackgroundsChooseWhiteText() {
        assertEquals(0xFFFFFFFFL,EditorState(bgMode=BackgroundMode.Image).withAddedText(1).texts.single().color)
        listOf(0xFF201E1DL,0xFF1F3A5FL,0xFF2F6B4FL).forEach { color -> assertEquals(0xFFFFFFFFL,EditorState(bg=color).withAddedText(1).texts.single().color) }
        assertEquals(0xFF201E1DL,EditorState().withAddedText(1).texts.single().color)
    }
    @Test fun deletionOnlyRemovesTheSelectedLayerAndSelectsQr() {
        val state=EditorState().withAddedText(1).withAddedText(2).withDeletedText()
        assertEquals(listOf(1L),state.texts.map { it.id });assertEquals(Selection.Qr,state.selection)
    }
    @Test fun processDeathSnapshotPreservesEveryEditableValue() {
        val original=EditorState(type=ContentType.Wifi,ssid="Guest",wifiPass="secret",theme=EditorTheme.Dark,bgMode=BackgroundMode.Image,bgImageReference="/cache/photo.png",ratio=CanvasRatio.Wide,grad=true,gradDir=GradientDirection.Up).withAddedText(10)
        val json=Json { encodeDefaults=true }
        assertEquals(original,json.decodeFromString<EditorState>(json.encodeToString(original)))
    }
}
