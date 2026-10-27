@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.qr.ui.studio

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.free.qr.R
import com.qr.data.PosterRenderer
import com.qr.domain.*
import java.text.NumberFormat

private enum class LayerPanel { PHOTO, OVERLAY, QR, EXPORT }
private val swatches = listOf(0xFF203E37, 0xFFB2C794, 0xFF15243F, 0xFF6B9EC5, 0xFF542D55, 0xFFE9A46D, 0xFF181818, 0xFFFFFFFF)

@Composable
internal fun PosterStudio(state: StudioState, dispatch: (StudioIntent) -> Unit, showPreview: Boolean = true) {
    var panel by rememberSaveable { mutableStateOf(LayerPanel.PHOTO) }
    var qrStyle by rememberSaveable { mutableStateOf(false) }
    val design = state.draft.poster
    val latest by rememberUpdatedState(design)
    val send by rememberUpdatedState(dispatch)
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    fun change(value: PosterDesign) = dispatch(StudioIntent.Poster(value))
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { dispatch(StudioIntent.Undo) }, enabled = state.canUndo && !state.working) { Text(stringResource(R.string.undo)) }
            TextButton(onClick = { dispatch(StudioIntent.Redo) }, enabled = state.canRedo && !state.working) { Text(stringResource(R.string.redo)) }
        }
        if (showPreview) {
        if (state.grid == null) {
            Column(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(20.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.studio_needs_qr), color = Muted)
                Button(onClick = { dispatch(StudioIntent.Navigate(StudioPage.CREATE)) }) { Text(stringResource(R.string.nav_create)) }
            }
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth().background(Tile, RoundedCornerShape(20.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                val width = minOf(maxWidth, 350.dp * design.ratio.aspect)
                Box(Modifier.width(width).aspectRatio(design.ratio.aspect).onSizeChanged { imageSize = it }
                    .pointerInput(Unit) {
                        var active = false
                        detectDragGestures(onDragStart = { point ->
                            active = PosterRenderer.geometry(imageSize.width.toFloat(), imageSize.height.toFloat(), latest).contains(point.x, point.y)
                        }, onDragEnd = { active = false }, onDragCancel = { active = false }) { pointer, delta ->
                            if (active) {
                                pointer.consume()
                                val p = latest
                                val side = imageSize.width * p.qrScale
                                val availableX = (imageSize.width * .92f - side).coerceAtLeast(1f)
                                val availableY = (imageSize.height - imageSize.width * .08f - side * if (p.caption.isBlank()) 1f else 1.19f).coerceAtLeast(1f)
                                send(StudioIntent.Poster(p.copy(x = (p.x + delta.x / availableX).coerceIn(0f,1f), y = (p.y + delta.y / availableY).coerceIn(0f,1f))))
                            }
                        }
                    }, contentAlignment = Alignment.Center) {
                    if (state.posterPreview != null) Image(state.posterPreview.asImageBitmap(), stringResource(R.string.poster_preview), Modifier.fillMaxSize())
                    else if (state.rendering) CircularProgressIndicator(Modifier.size(28.dp))
                    else Text(stringResource(R.string.preview_prompt), Modifier.padding(20.dp), color = Muted)
                    if (state.rendering && state.posterPreview != null) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.BottomCenter))
                }
            }
            Text(stringResource(R.string.drag_qr_hint), color = Muted, fontSize = 11.sp)
        }
        }
        state.posterError?.let { Text(it.resolve(LocalContext.current), color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LayerPanel.entries.forEach { item -> FilterChip(selected = panel == item, onClick = { panel = item }, label = { Text(stringResource(when (item) {
                LayerPanel.PHOTO -> R.string.layer_photo; LayerPanel.OVERLAY -> R.string.layer_overlay; LayerPanel.QR -> R.string.layer_qr; LayerPanel.EXPORT -> R.string.tab_export
            })) }) }
        }
        Column(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(20.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (panel) {
                LayerPanel.PHOTO -> {
                    Text(stringResource(R.string.background_photo), fontWeight = FontWeight.SemiBold)
                    Button(onClick = { dispatch(StudioIntent.ChoosePhoto) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(if (design.photoUri == null) R.string.choose_photo else R.string.replace_photo)) }
                    if (design.photoUri != null) {
                        TextButton(onClick = { change(design.copy(photoUri = null)) }) { Text(stringResource(R.string.remove_photo)) }
                        Text(stringResource(R.string.photo_zoom), fontSize = 13.sp)
                        Slider(value = design.photoZoom, onValueChange = { change(design.copy(photoZoom = it)) }, valueRange = 1f..3f)
                    }
                    Text(stringResource(R.string.canvas_size), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CanvasRatio.entries.forEach { ratio -> FilterChip(selected = design.ratio == ratio, onClick = { change(design.copy(ratio = ratio)) }, label = { Text(stringResource(when (ratio) { CanvasRatio.SQUARE -> R.string.ratio_square; CanvasRatio.PORTRAIT -> R.string.ratio_portrait; CanvasRatio.STORY -> R.string.ratio_story }), fontSize = 12.sp) }) }
                    }
                    Text(stringResource(R.string.photo_private_note), color = Muted, fontSize = 12.sp)
                }
                LayerPanel.OVERLAY -> {
                    Toggle(stringResource(R.string.gradient_overlay), design.gradientEnabled) { change(design.copy(gradientEnabled = it)) }
                    Text(stringResource(R.string.top_color), fontSize = 13.sp)
                    ColorSwatches(design.topColor) { change(design.copy(topColor = it)) }
                    Text(stringResource(R.string.bottom_color), fontSize = 13.sp)
                    ColorSwatches(design.bottomColor) { change(design.copy(bottomColor = it)) }
                    Text(stringResource(R.string.overlay_opacity, NumberFormat.getPercentInstance().format(design.opacity)), fontSize = 13.sp)
                    Slider(value = design.opacity, onValueChange = { change(design.copy(opacity = it)) }, valueRange = 0f..1f)
                    OutlinedButton(onClick = { change(design.copy(reversed = !design.reversed)) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.reverse_gradient)) }
                    Text(stringResource(R.string.templates), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { change(design.copy(topColor = 0xFF203E37, bottomColor = 0xFFB2C794, reversed = false, frame = QrFrame.CARD)) }, label = { Text(stringResource(R.string.template_forest)) })
                        AssistChip(onClick = { change(design.copy(topColor = 0xFF542D55, bottomColor = 0xFFE9A46D, reversed = false, frame = QrFrame.OUTLINE)) }, label = { Text(stringResource(R.string.template_sunset)) })
                        AssistChip(onClick = { change(design.copy(topColor = 0xFF15243F, bottomColor = 0xFF6B9EC5, reversed = false, frame = QrFrame.DOUBLE)) }, label = { Text(stringResource(R.string.template_ocean)) })
                    }
                }
                LayerPanel.QR -> {
                    Text(stringResource(R.string.qr_size), fontWeight = FontWeight.SemiBold)
                    Slider(value = design.qrScale, onValueChange = { change(design.copy(qrScale = it)) }, valueRange = .26f.. .76f)
                    Text(stringResource(R.string.qr_position), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0f to R.string.position_top, .5f to R.string.position_center, 1f to R.string.position_bottom).forEach { (position, label) ->
                            AssistChip(onClick = { change(design.copy(x = .5f, y = position)) }, label = { Text(stringResource(label)) })
                        }
                    }
                    Text(stringResource(R.string.qr_frame), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QrFrame.entries.forEach { frame -> FilterChip(selected = design.frame == frame, onClick = { change(design.copy(frame = frame)) }, label = { Text(stringResource(when (frame) { QrFrame.NONE -> R.string.frame_none; QrFrame.OUTLINE -> R.string.frame_outline; QrFrame.DOUBLE -> R.string.frame_double; QrFrame.CARD -> R.string.frame_card })) }) }
                    }
                    OutlinedTextField(value = design.caption, onValueChange = { change(design.copy(caption = it)) }, label = { Text(stringResource(R.string.qr_caption)) }, placeholder = { Text(stringResource(R.string.scan_me)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    AssistChip(onClick = { change(design.copy(caption = "")) }, label = { Text(stringResource(R.string.remove_caption)) })
                    OutlinedButton(onClick = { qrStyle = !qrStyle }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.qr_colors_shapes)) }
                    if (qrStyle) DesignPanel(state,dispatch)
                }
                LayerPanel.EXPORT -> ExportPanel(state, dispatch, composition = state.draft.poster.enabled)
            }
        }
        Button(onClick = { dispatch(StudioIntent.Save) }, enabled = state.grid != null && state.posterError == null && !state.rendering && !state.working && state.libraryReady, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.save_design)) }
    }

}
@Composable private fun Toggle(title: String, value: Boolean, update: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Switch(value, update) }
}
@Composable private fun ColorSwatches(selected: Long, update: (Long) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        swatches.forEach { color ->
            Box(Modifier.size(34.dp).background(Color(color), CircleShape).border(if (selected == color) 3.dp else 1.dp, if (selected == color) MaterialTheme.colorScheme.primary else Line, CircleShape)
                .clickable(onClickLabel = stringResource(R.string.select_color, "#%06X".format(color and 0xFFFFFF))) { update(color) })
        }
    }
}
