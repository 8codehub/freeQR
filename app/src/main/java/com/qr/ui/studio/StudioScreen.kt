@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.qr.ui.studio

import com.free.qr.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import com.qr.ui.studio.icons.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qr.domain.*
import java.text.DateFormat
import java.util.Date

private data class StudioColors(val ink: Color, val green: Color, val muted: Color, val paper: Color, val line: Color, val lime: Color, val card: Color, val tile: Color, val preview: Color)
private val DayColors = StudioColors(Color(0xFF182E27), Color(0xFF315F49), Color(0xFF748078), Color(0xFFF7F7F2), Color(0xFFE1E5DB), Color(0xFFE4EFC4), Color.White, Color(0xFFEAEEE5), Color(0xFFEBEFE3))
private val NightColors = StudioColors(Color(0xFFE8EEE3), Color(0xFFB8D397), Color(0xFFA5B2A6), Color(0xFF121B17), Color(0xFF34443A), Color(0xFF344E32), Color(0xFF1D2A22), Color(0xFF263329), Color(0xFF223327))
private val LocalStudioColors = staticCompositionLocalOf { DayColors }
internal val Ink: Color @Composable get() = LocalStudioColors.current.ink
internal val Green: Color @Composable get() = LocalStudioColors.current.green
internal val Muted: Color @Composable get() = LocalStudioColors.current.muted
internal val Paper: Color @Composable get() = LocalStudioColors.current.paper
internal val Line: Color @Composable get() = LocalStudioColors.current.line
internal val Lime: Color @Composable get() = LocalStudioColors.current.lime
internal val Card: Color @Composable get() = LocalStudioColors.current.card
internal val Tile: Color @Composable get() = LocalStudioColors.current.tile
internal val PreviewColor: Color @Composable get() = LocalStudioColors.current.preview
internal val CardShape = RoundedCornerShape(24.dp)

@Composable
fun StudioTheme(appearance: Appearance = Appearance.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (appearance) { Appearance.SYSTEM -> isSystemInDarkTheme(); Appearance.LIGHT -> false; Appearance.DARK -> true }
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        }
    }
    com.qr.ui.theme.EasyQRStudioTheme(darkTheme = dark) {
        val scheme = MaterialTheme.colorScheme
        val colors = StudioColors(scheme.onSurface, scheme.primary, scheme.onSurfaceVariant,
            scheme.background, scheme.outlineVariant, scheme.secondaryContainer, scheme.surface,
            scheme.surfaceVariant, scheme.surfaceVariant)
        CompositionLocalProvider(LocalStudioColors provides colors, content = content)
    }
}

private fun ContentType.icon(): ImageVector = when (this) {
    ContentType.URL -> Icons.Outlined.Link
    ContentType.TEXT -> Icons.AutoMirrored.Outlined.Notes
    ContentType.WIFI -> Icons.Outlined.Wifi
    ContentType.EMAIL -> Icons.Outlined.MailOutline
    ContentType.PHONE -> Icons.Outlined.Call
    ContentType.SMS -> Icons.Outlined.ChatBubbleOutline
    ContentType.CONTACT -> Icons.Outlined.PersonOutline
}

@Composable
fun StudioScreen(state: StudioState, snackbar: SnackbarHostState, dispatch: (StudioIntent) -> Unit) {
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, topBar = {
        CenterAlignedTopAppBar(title = { Text(stringResource(R.string.legacy_title), fontWeight = FontWeight.SemiBold) }, actions = {
            IconButton(onClick = { dispatch(StudioIntent.Navigate(StudioPage.LIBRARY)) }) { Icon(Icons.Outlined.Bookmarks, stringResource(R.string.nav_library)) }
            IconButton(onClick = { dispatch(StudioIntent.Navigate(StudioPage.GUIDE)) }) { Icon(Icons.Filled.Settings, stringResource(R.string.settings)) }
        })
    }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) { LegacyGenerator(state, dispatch) }
    }
    if (state.page != StudioPage.CREATE) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(onDismissRequest = { dispatch(StudioIntent.Navigate(StudioPage.CREATE)) }, sheetState = sheet, scrimColor = if (state.page == StudioPage.DESIGN) Color.Transparent else MaterialTheme.colorScheme.scrim.copy(alpha = .32f)) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(.65f)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(when (state.page) {
                        StudioPage.DESIGN -> R.string.photo_studio_title; StudioPage.LIBRARY -> R.string.nav_library;
                        else -> R.string.settings
                    }), Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    if (state.page == StudioPage.DESIGN) TextButton(onClick = { dispatch(StudioIntent.ResetDesign) }) { Text(stringResource(R.string.reset_design)) }
                    TextButton(onClick = { dispatch(StudioIntent.Navigate(StudioPage.CREATE)) }) { Text(stringResource(R.string.done)) }
                }
                Box(Modifier.weight(1f)) {
                    when (state.page) {
                        StudioPage.DESIGN -> PosterStudio(state, dispatch, showPreview = false)
                        StudioPage.LIBRARY -> Library(state, dispatch)
                        StudioPage.GUIDE -> LegacySettings(state.appearance, dispatch)
                        else -> Unit
                    }
                }
            }
        }
    }
}
@Composable
internal fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.5).sp)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}
@Composable
internal fun ContentPanel(state: StudioState, dispatch: (StudioIntent) -> Unit, compact: Boolean = false) {
    if (!compact) SectionTitle(stringResource(R.string.content_title), stringResource(R.string.content_subtitle))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ContentType.entries.forEach { type ->
            val selected = state.draft.type == type
            Column(Modifier.width(74.dp).clip(RoundedCornerShape(16.dp)).background(if (selected) Ink else Card)
                .clickable { dispatch(StudioIntent.Type(type)) }.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(type.icon(), null, tint = if (selected) Lime else Muted, modifier = Modifier.size(23.dp))
                Spacer(Modifier.height(8.dp))
                Text(stringResource(type.titleRes), fontSize = 11.sp, color = if (selected) Paper else Ink)
            }
        }
    }
    Column(Modifier.fillMaxWidth().clip(CardShape).background(Card).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (!compact) Text(stringResource(state.draft.type.hintRes), fontFamily = FontFamily.Serif, fontSize = 22.sp)
        state.draft.type.fields().forEach { spec ->
            var reveal by remember(spec.key, state.draft.type) { mutableStateOf(false) }
            val password = spec.key == "password"
            val keyboard = when (spec.key) { "url" -> KeyboardType.Uri; "email" -> KeyboardType.Email; "phone" -> KeyboardType.Phone; "password" -> KeyboardType.Password; else -> KeyboardType.Text }
            OutlinedTextField(value = state.draft.fields[spec.key].orEmpty(), onValueChange = { if (it.length <= 2000) dispatch(StudioIntent.Field(spec.key, it)) },
                modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(spec.labelRes) + if (spec.required) " *" else "") },
                placeholder = { Text(stringResource(spec.placeholderRes), fontSize = 13.sp) }, shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(textDirection = if (spec.key in listOf("url", "email", "phone")) androidx.compose.ui.text.style.TextDirection.Ltr else androidx.compose.ui.text.style.TextDirection.Content),
                keyboardOptions = KeyboardOptions(keyboardType = keyboard), singleLine = spec.key !in listOf("text", "body"),
                minLines = if (spec.key in listOf("text", "body")) 3 else 1,
                visualTransformation = if (password && !reveal) PasswordVisualTransformation() else VisualTransformation.None,
                trailingIcon = if (password) { { IconButton(onClick = { reveal = !reveal }) { Icon(if (reveal) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (reveal) stringResource(R.string.hide_password) else stringResource(R.string.show_password)) } } } else null)
        }
        if (state.draft.type == ContentType.WIFI) {
            SwitchRow(stringResource(R.string.open_network), state.draft.openWifi) { dispatch(StudioIntent.Wifi(it, state.draft.hiddenWifi)) }
            SwitchRow(stringResource(R.string.hidden_network), state.draft.hiddenWifi) { dispatch(StudioIntent.Wifi(state.draft.openWifi, it)) }
            Text(stringResource(R.string.wifi_note), fontSize = 11.sp, color = Muted)
        }
    }
}
@Composable
private fun SwitchRow(title: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontSize = 13.sp); Switch(checked, onCheckedChange = change)
    }
}
@Composable
internal fun DesignPanel(state: StudioState, dispatch: (StudioIntent) -> Unit) {
    SectionTitle(stringResource(R.string.design_title), stringResource(R.string.design_subtitle))
    Column(Modifier.fillMaxWidth().clip(CardShape).background(Card).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(stringResource(R.string.color_story), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            palettes.forEachIndexed { index, palette ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { dispatch(StudioIntent.Color(index)) }) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Color(palette.ink)).border(if (state.draft.palette == index) 3.dp else 0.dp, Color(0xFFACBC98), CircleShape), contentAlignment = Alignment.Center) {
                        if (state.draft.palette == index) Icon(Icons.Outlined.Check, stringResource(R.string.selected_palette, stringResource(palette.titleRes)), tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(6.dp)); Text(stringResource(palette.titleRes), fontSize = 10.sp)
                }
            }
        }
        HorizontalDivider(color = Line)
        Text(stringResource(R.string.module_shape), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModuleShape.entries.forEach { shape ->
                FilterChip(selected = state.draft.shape == shape, onClick = { dispatch(StudioIntent.Shape(shape)) }, label = { Text(shape.label(), fontSize = 12.sp) })
            }
        }
        Text(stringResource(R.string.finder_note), color = Muted, fontSize = 12.sp)
        HorizontalDivider(color = Line)
        Text(stringResource(R.string.error_recovery), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Correction.entries.forEach { correction ->
                FilterChip(selected = state.draft.correction == correction, onClick = { dispatch(StudioIntent.Reliability(correction)) }, label = { Text(when (correction) { Correction.M -> stringResource(R.string.recovery_medium); Correction.Q -> stringResource(R.string.recovery_quartile); Correction.H -> stringResource(R.string.recovery_high) }) })
            }
        }
        Text(stringResource(R.string.recovery_note), color = Muted, fontSize = 12.sp)
    }
}
@Composable
internal fun ExportPanel(state: StudioState, dispatch: (StudioIntent) -> Unit, composition: Boolean = false) {
    SectionTitle(stringResource(R.string.export_title), stringResource(R.string.export_subtitle))
    Column(Modifier.fillMaxWidth().clip(CardShape).background(Card).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(value = state.draft.title, onValueChange = { dispatch(StudioIntent.Title(it)) }, label = { Text(stringResource(R.string.qr_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
        Text(stringResource(R.string.png_resolution), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(512, 1024, 2048).forEach { size -> FilterChip(selected = state.draft.size == size,
                onClick = { dispatch(StudioIntent.Size(size)) }, label = { Text(stringResource(R.string.pixel_size, size), fontSize = 12.sp) }) }
        }
        val ready = state.grid != null && !state.rendering && !state.working && (!composition || (state.posterError == null && state.posterPreview != null))
        Button(onClick = { dispatch(StudioIntent.Export(ExportFormat.PNG, composition = composition)) }, enabled = ready, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Outlined.FileDownload, null, Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.download_png))
        }
        OutlinedButton(onClick = { dispatch(StudioIntent.Export(ExportFormat.SVG)) }, enabled = ready, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Outlined.Code, null, Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(if (composition) R.string.export_qr_svg else R.string.download_svg))
        }
        TextButton(onClick = { dispatch(StudioIntent.Export(ExportFormat.PNG, share = true, composition = composition)) }, enabled = ready, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(Icons.Outlined.IosShare, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.share_image))
        }
        if (state.working) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(stringResource(if (composition) R.string.poster_export_note else R.string.export_note), color = Muted, fontSize = 12.sp)
    }
}
@Composable
private fun Library(state: StudioState, dispatch: (StudioIntent) -> Unit) {
    var deleting by remember { mutableStateOf<SavedQr?>(null) }
    val context = LocalContext.current
    val items = state.library.filter { it.draft.title.ifBlank { context.getString(R.string.untitled_qr) }.contains(state.query, true) || context.getString(it.draft.type.titleRes).contains(state.query, true) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(stringResource(R.string.library_title), fontFamily = FontFamily.Serif, fontSize = 34.sp, letterSpacing = (-1).sp)
        Text(stringResource(R.string.library_subtitle), color = Muted, fontSize = 13.sp)
        OutlinedTextField(state.query, { dispatch(StudioIntent.Search(it)) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text(stringResource(R.string.search_hint)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, shape = RoundedCornerShape(16.dp))
        Text(stringResource(R.string.saved_count, state.library.size), color = Muted, fontSize = 11.sp)
        if (!state.libraryReady) Text(stringResource(R.string.library_unavailable), color = Muted)
        else if (items.isEmpty()) {
            Column(Modifier.fillMaxWidth().clip(CardShape).background(Card).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Bookmarks, null, Modifier.size(44.dp), tint = Green)
                Spacer(Modifier.height(16.dp)); Text(if (state.query.isBlank()) stringResource(R.string.library_empty_title) else stringResource(R.string.no_matches), fontFamily = FontFamily.Serif, fontSize = 23.sp)
                Spacer(Modifier.height(8.dp)); Text(if (state.query.isBlank()) stringResource(R.string.library_empty_body) else stringResource(R.string.search_empty_body), color = Muted, fontSize = 12.sp)
            }
        }
        items.forEach { item ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Card).clickable { dispatch(StudioIntent.Open(item)) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(palettes[item.draft.palette].paper)), contentAlignment = Alignment.Center) {
                    Icon(item.draft.type.icon(), null, tint = Color(palettes[item.draft.palette].ink), modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.draft.title.ifBlank { stringResource(R.string.untitled_qr) }, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    Text(stringResource(R.string.library_type_date, stringResource(item.draft.type.titleRes), DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(item.updatedAt))), fontSize = 11.sp, color = Muted)
                }
                IconButton(onClick = { deleting = item }, enabled = !state.working) { Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.delete_description, item.draft.title.ifBlank { stringResource(R.string.untitled_qr) }), tint = Muted) }
            }
        }
    }
    deleting?.let { item -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text(stringResource(R.string.delete_title)) },
        text = { Text(stringResource(R.string.delete_body, item.draft.title.ifBlank { stringResource(R.string.untitled_qr) })) },
        confirmButton = { TextButton(onClick = { dispatch(StudioIntent.Delete(item.id)); deleting = null }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.keep_qr)) } }) }
}
@Composable
private fun Guide(appearance: Appearance, dispatch: (StudioIntent) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Text(stringResource(R.string.guide_title), fontFamily = FontFamily.Serif, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-1).sp)
        Text(stringResource(R.string.guide_subtitle), color = Muted, fontSize = 13.sp)
        Column(Modifier.fillMaxWidth().clip(CardShape).background(Card).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.appearance_title), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(stringResource(R.string.appearance), color = Muted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Appearance.entries.forEach { mode ->
                    FilterChip(selected = appearance == mode, onClick = { dispatch(StudioIntent.SetAppearance(mode)) }, label = { Text(mode.label()) })
                }
            }
        }
        listOf(
            Triple(Icons.Outlined.Contrast, stringResource(R.string.guide_contrast_title), stringResource(R.string.guide_contrast_body)),
            Triple(Icons.Outlined.Print, stringResource(R.string.guide_print_title), stringResource(R.string.guide_print_body)),
            Triple(Icons.Outlined.Verified, stringResource(R.string.guide_test_title), stringResource(R.string.guide_test_body)),
            Triple(Icons.Outlined.Lock, stringResource(R.string.guide_privacy_title), stringResource(R.string.guide_privacy_body)),
            Triple(Icons.Outlined.AllInclusive, stringResource(R.string.guide_static_title), stringResource(R.string.guide_static_body)),
            Triple(Icons.Outlined.Info, stringResource(R.string.guide_about_title), stringResource(R.string.guide_about_body))
        ).forEach { (icon, title, body) ->
            Row(Modifier.fillMaxWidth().clip(CardShape).background(Card).padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(icon, null, tint = Green, modifier = Modifier.size(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(body, color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Easy QR Studio", showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun StudioPreview() {
    val draft = QrDraft(title = "My website", fields = mapOf("url" to "https://example.com"), palette = 1)
    val engine = remember { QrEngine() }
    val grid = remember { engine.generate(draft) }
    val bitmap = remember { engine.bitmap(grid, draft, 640) }
    StudioTheme {
        StudioScreen(StudioState(draft = draft, grid = grid, preview = bitmap, rendering = false, libraryReady = true), remember { SnackbarHostState() }, {})
    }
}

@Composable private fun EditorTab.label() = stringResource(when (this) {
    EditorTab.CONTENT -> R.string.tab_content; EditorTab.DESIGN -> R.string.tab_design; EditorTab.EXPORT -> R.string.tab_export
})
@Composable private fun ModuleShape.label() = stringResource(when (this) {
    ModuleShape.SQUARE -> R.string.shape_square; ModuleShape.ROUNDED -> R.string.shape_rounded; ModuleShape.DOTS -> R.string.shape_dots
})
@Composable private fun Appearance.label() = stringResource(when (this) {
    Appearance.SYSTEM -> R.string.appearance_system; Appearance.LIGHT -> R.string.appearance_light; Appearance.DARK -> R.string.appearance_dark
})
