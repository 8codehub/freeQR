package qrstudio.model

fun EditorState.withAddedText(timestamp: Long,text: String?=null): EditorState {
    val layer=TextLayer(id=maxOf(timestamp,(texts.maxOfOrNull { it.id } ?: 0L)+1),y=if(texts.size%2==0) .12f else .88f,
        color=if(bgMode==BackgroundMode.Image || bg in setOf(0xFF201E1D,0xFF1F3A5F,0xFF2F6B4F)) 0xFFFFFFFF else 0xFF201E1D)
        .let { if(text!=null) it.copy(text=text) else it }
    return copy(texts=texts+layer,selection=Selection.Text(layer.id),tab=EditorTab.Text)
}
fun EditorState.withDeletedText(): EditorState {
    val selected=selection as? Selection.Text ?: return this
    return copy(texts=texts.filterNot { it.id==selected.id },selection=Selection.Qr)
}
