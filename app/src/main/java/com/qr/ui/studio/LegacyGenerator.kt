@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.qr.ui.studio

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.qr.data.PosterRenderer
import kotlin.math.min
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.free.qr.R
import com.qr.domain.*
import com.qr.ui.studio.icons.*

@Composable
internal fun LegacyGenerator(state: StudioState, dispatch: (StudioIntent) -> Unit) {
    var tools by rememberSaveable { mutableStateOf(false) }
    val mainScroll = rememberScrollState()
    LaunchedEffect(state.page) { if (state.page == StudioPage.DESIGN) mainScroll.animateScrollTo(0) }
    val qrColors = listOf(
        stringResource(R.string.color_black) to ComposeColor(0xFF000000),
        stringResource(R.string.color_red) to ComposeColor(0xFFE53935),
        stringResource(R.string.color_green) to ComposeColor(0xFF43A047),
        stringResource(R.string.color_blue) to ComposeColor(0xFF1E88E5),
        stringResource(R.string.color_magenta) to ComposeColor(0xFFD500F9))
    val bgColors = listOf(
        stringResource(R.string.color_white) to ComposeColor.White,
        stringResource(R.string.color_cyan) to ComposeColor(0xFFB2EBF2),
        stringResource(R.string.color_yellow) to ComposeColor(0xFFFFF59D),
        stringResource(R.string.color_gray) to ComposeColor(0xFFE0E0E0),
        stringResource(R.string.color_transparent) to ComposeColor.Transparent)
    val selectedQr = qrColors.firstOrNull { (it.second.toArgb().toLong() and 0xFFFFFFFF) == state.draft.colors.ink } ?: ("" to ComposeColor(state.draft.colors.ink))
    val selectedBg = bgColors.firstOrNull { (it.second.toArgb().toLong() and 0xFFFFFFFF) == state.draft.colors.paper } ?: ("" to ComposeColor(state.draft.colors.paper))
    val ready = state.grid != null && !state.rendering && !state.working && (!state.draft.poster.enabled || (state.posterPreview != null && state.posterError == null))
    Column(Modifier.fillMaxSize().verticalScroll(mainScroll).padding(horizontal = 20.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        QrPreview(if (state.draft.poster.enabled) state.posterPreview else state.preview, state.draft.poster.takeIf { it.enabled && state.grid != null }, compact = state.page == StudioPage.DESIGN) { dispatch(StudioIntent.Poster(it)) }
        state.posterError?.let { Text(it.resolve(LocalContext.current),color = MaterialTheme.colorScheme.error,fontSize = 12.sp) }
        if (state.draft.type in listOf(ContentType.URL, ContentType.TEXT)) {
            val key = if (state.draft.type == ContentType.URL) "url" else "text"
            val text = state.draft.fields[key].orEmpty()
            OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = text,
                onValueChange = {
                    if (it.length <= 2000) {
                        if (state.draft.type != ContentType.TEXT) dispatch(StudioIntent.Type(ContentType.TEXT))
                        dispatch(StudioIntent.Field("text", it))
                    }
                }, label = { Text(stringResource(R.string.enter_text_link)) }, placeholder = { Text(stringResource(R.string.legacy_example)) },
                singleLine = false, maxLines = 4, shape = RoundedCornerShape(16.dp), trailingIcon = {
                    if (text.isNotEmpty()) IconButton(onClick = { dispatch(StudioIntent.Field(key, "")) }) { Icon(Icons.Filled.Clear, stringResource(R.string.clear_text)) }
                })
        } else {
            ContentPanel(state,dispatch,compact = true)
        }
        if (state.error != null && state.draft.fields.values.any { it.isNotBlank() }) Text(state.error.resolve(LocalContext.current), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = { dispatch(StudioIntent.Export(ExportFormat.PNG,share = true,composition = state.draft.poster.enabled)) }, enabled = ready,
                modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Filled.Share,null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.share))
            }
            Button(onClick = { dispatch(StudioIntent.Export(ExportFormat.PNG,gallery = true,composition = state.draft.poster.enabled)) }, enabled = ready,
                modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Filled.Download,null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.save))
            }
        }
        ColorSection(stringResource(R.string.qr_color),qrColors,selectedQr) { dispatch(StudioIntent.Ink(it.second.toArgb().toLong() and 0xFFFFFFFF)) }
        ColorSection(stringResource(R.string.background),bgColors,selectedBg) { dispatch(StudioIntent.Paper(it.second.toArgb().toLong() and 0xFFFFFFFF)) }
        OutlinedButton(onClick = { dispatch(StudioIntent.Navigate(StudioPage.DESIGN)) },modifier = Modifier.fillMaxWidth().height(52.dp),shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Outlined.AutoAwesome,null,Modifier.size(20.dp));Spacer(Modifier.width(8.dp));Text(stringResource(R.string.open_design_studio))
        }
        TextButton(onClick = { tools = true }) { Text(stringResource(R.string.more_tools)) }
        Spacer(Modifier.height(4.dp))
    }
    if (tools) ModalBottomSheet(onDismissRequest = { tools = false }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp).padding(bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.more_tools), style = MaterialTheme.typography.titleLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContentType.entries.forEach { type -> FilterChip(selected = state.draft.type == type, onClick = { dispatch(StudioIntent.Type(type)); tools = false },label = { Text(stringResource(type.titleRes)) }) }
            }
            Button(onClick = { dispatch(StudioIntent.Save) }, enabled = ready && state.libraryReady,modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save_design)) }
            OutlinedButton(onClick = { dispatch(StudioIntent.New); tools = false },modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.new_qr)) }
            DesignPanel(state,dispatch)
            ExportPanel(state,dispatch,composition = state.draft.poster.enabled)
        }
    }
}

@Composable
internal fun LegacySettings(appearance: Appearance, dispatch: (StudioIntent) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp,vertical = 12.dp)) {
        Text(stringResource(R.string.appearance),style = MaterialTheme.typography.titleSmall,fontWeight = FontWeight.SemiBold,color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Surface(Modifier.fillMaxWidth(),shape = RoundedCornerShape(20.dp),color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)) {
            Column(Modifier.selectableGroup()) {
                ThemeModeOption(stringResource(R.string.system_default),stringResource(R.string.follow_device_theme),Icons.Filled.BrightnessAuto,appearance == Appearance.SYSTEM) { dispatch(StudioIntent.SetAppearance(Appearance.SYSTEM)) }
                ThemeModeOption(stringResource(R.string.appearance_light),stringResource(R.string.always_light_theme),Icons.Filled.LightMode,appearance == Appearance.LIGHT) { dispatch(StudioIntent.SetAppearance(Appearance.LIGHT)) }
                ThemeModeOption(stringResource(R.string.appearance_dark),stringResource(R.string.always_dark_theme),Icons.Filled.DarkMode,appearance == Appearance.DARK) { dispatch(StudioIntent.SetAppearance(Appearance.DARK)) }
            }
        }
    }
}

@Composable
private fun ThemeModeOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(selected = selected, onClick = null)
    }
}

@Composable
private fun QrPreview(bitmap: Bitmap?, design: PosterDesign? = null, compact: Boolean = false, onPosition: (PosterDesign) -> Unit = {}) {
    val current by rememberUpdatedState(design)
    val image by rememberUpdatedState(bitmap)
    val update by rememberUpdatedState(onPosition)
    var dimensions by remember { mutableStateOf(IntSize.Zero) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (compact) Modifier.height(minOf(230.dp, androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * .28f)) else Modifier.aspectRatio(1f)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ComposeColor.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.legacy_generated_qr),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .onSizeChanged { dimensions = it }
                            .pointerInput(Unit) {
                                var active = false
                                fun fitted(): FloatArray {
                                    val bmp = image ?: return floatArrayOf(0f,0f,0f,0f)
                                    val factor = min(dimensions.width.toFloat()/bmp.width,dimensions.height.toFloat()/bmp.height)
                                    val width = bmp.width*factor; val height = bmp.height*factor
                                    return floatArrayOf(width,height,(dimensions.width-width)/2,(dimensions.height-height)/2)
                                }
                                detectDragGestures(onDragStart = { point ->
                                    val p = current
                                    val f = fitted()
                                    active = p != null && PosterRenderer.geometry(f[0],f[1],p).contains(point.x-f[2],point.y-f[3])
                                },onDragEnd = { active = false },onDragCancel = { active = false }) { pointer,delta ->
                                    val p = current
                                    if (active && p != null) {
                                        pointer.consume()
                                        val f = fitted();val side=f[0]*p.qrScale
                                        val ax=(f[0]*.92f-side).coerceAtLeast(1f)
                                        val ay=(f[1]-f[0]*.08f-side*(if (p.caption.isBlank()) 1f else 1.19f)).coerceAtLeast(1f)
                                        update(p.copy(x=(p.x+delta.x/ax).coerceIn(0f,1f),y=(p.y+delta.y/ay).coerceIn(0f,1f)))
                                    }
                                }
                            }
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = stringResource(R.string.legacy_preview_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun ColorSection(
    title: String,
    colorOptions: List<Pair<String, ComposeColor>>,
    selected: Pair<String, ComposeColor>,
    onSelect: (Pair<String, ComposeColor>) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            colorOptions.forEach { option ->
                val (name, color) = option
                val isSelected = selected.first == name
                ColorSwatch(
                    color = color,
                    isSelected = isSelected,
                    modifier = Modifier.weight(1f).semantics { contentDescription = name; this.selected = isSelected },
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: ComposeColor,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isTransparent = color.alpha == 0f
    // Pick a check-mark tint that contrasts with the swatch.
    val checkTint = if (isTransparent || color.luminance() > 0.5f) {
        ComposeColor.Black
    } else {
        ComposeColor.White
    }
    val ringColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(if (isTransparent) MaterialTheme.colorScheme.surface else color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = ringColor,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isTransparent) {
            Text(
                text = stringResource(R.string.transparent_dash),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = stringResource(R.string.selected),
                tint = checkTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
