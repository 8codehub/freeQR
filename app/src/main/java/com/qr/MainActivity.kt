package com.qr

import com.free.qr.R
import com.qr.domain.resolve
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.qr.ui.studio.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val model: StudioViewModel by viewModels()
    private val snackbar = SnackbarHostState()
    private val savePng = registerForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri -> saveExport(uri) }
    private val saveSvg = registerForActivityResult(ActivityResultContracts.CreateDocument("image/svg+xml")) { uri -> saveExport(uri) }
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                model.dispatch(StudioIntent.Poster(model.state.value.draft.poster.copy(photoUri = uri.toString())))
            } catch (e: Exception) { model.message(R.string.error_photo) }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            StudioTheme(state.appearance) {
                StudioScreen(state, snackbar, model::dispatch)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.effects.collect { effect ->
                    when (effect) {
                        StudioEffect.PickPhoto -> try { pickPhoto.launch(arrayOf("image/*")) } catch (e: Exception) { model.message(R.string.error_photo) }
                        is StudioEffect.Message -> launch { snackbar.showSnackbar(effect.text.resolve(this@MainActivity)) }
                        is StudioEffect.Export -> {
                            if (effect.share) share(effect.artifact)
                            else if (effect.gallery && android.os.Build.VERSION.SDK_INT >= 29) saveGallery(effect.artifact)
                            else try {
                                if (effect.artifact.mime == "image/png") savePng.launch(effect.artifact.name) else saveSvg.launch(effect.artifact.name)
                            } catch (e: Exception) { model.message(R.string.error_file_app) }
                        }
                    }
                }
            }
        }
    }
    @androidx.annotation.RequiresApi(29)
    private suspend fun saveGallery(artifact: com.qr.domain.ExportArtifact) {
        var created: Uri? = null
        try {
            withContext(Dispatchers.IO) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, artifact.name)
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, artifact.mime)
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Easy QR Studio")
                    put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                }
                created = checkNotNull(contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
                checkNotNull(contentResolver.openOutputStream(created!!)).use { it.write(artifact.bytes) }
                values.clear(); values.put(android.provider.MediaStore.Images.Media.IS_PENDING,0)
                check(contentResolver.update(created!!,values,null,null) > 0)
            }
            model.message(R.string.saved_gallery)
        } catch (e: Exception) {
            created?.let { uri -> withContext(Dispatchers.IO) { runCatching { contentResolver.delete(uri,null,null) } } }
            model.message(R.string.error_save_file)
        } finally { model.pendingExport = null }
    }
    private fun saveExport(uri: Uri?) {
        if (uri == null) { model.pendingExport = null; return }
        val artifact = model.pendingExport
        if (artifact == null) { model.message(R.string.error_export_again); return }
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    checkNotNull(contentResolver.openOutputStream(uri)).use { it.write(artifact.bytes) }
                }
                model.message(R.string.export_success)
            } catch (e: Exception) { model.message(R.string.error_save_file) }
            finally { model.pendingExport = null }
        }
    }
    private suspend fun share(artifact: com.qr.domain.ExportArtifact) {
        try {
            val uri = withContext(Dispatchers.IO) {
                val directory = File(cacheDir, "exports").apply { mkdirs() }
                directory.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 86_400_000L }?.forEach { it.delete() }
                val file = File(directory, artifact.name).apply { writeBytes(artifact.bytes) }
                FileProvider.getUriForFile(this@MainActivity, "$packageName.files", file)
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = artifact.mime; putExtra(Intent.EXTRA_STREAM, uri)
                clipData = android.content.ClipData.newRawUri(getString(R.string.qr_image), uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_qr)))
        } catch (e: Exception) { model.message(R.string.error_share) }
        finally { model.pendingExport = null }
    }
}
