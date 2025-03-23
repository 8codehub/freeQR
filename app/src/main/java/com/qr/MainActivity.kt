package com.qr

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.qr.ui.theme.EasyQRStudioTheme
import java.io.File
import java.io.FileOutputStream
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color as ComposeColor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EasyQRStudioTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(color = Color.Gray)
                    ) {
                        QrCodeGeneratorUI()
                    }
                }
            }
        }
    }
}

@Composable
fun QrCodeGeneratorUI() {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }

    val qrColors = listOf(
        "Black" to ComposeColor.Black,
        "Red" to ComposeColor.Red,
        "Green" to ComposeColor.Green,
        "Blue" to ComposeColor.Blue,
        "Magenta" to ComposeColor.Magenta,
    )

    val bgColors = listOf(
        "Transparent" to ComposeColor.Transparent,
        "White" to ComposeColor.White,
        "Yellow" to ComposeColor.Yellow,
        "Light Gray" to ComposeColor.LightGray,
        "Cyan" to ComposeColor.Cyan
    )

    var selectedQrColor by remember { mutableStateOf(qrColors.first()) }
    var selectedBgColor by remember { mutableStateOf(bgColors.first()) }

    val bitmap = remember(text, selectedQrColor, selectedBgColor) {
        generateQrCode(
            text = text,
            codeColor = selectedQrColor.second.toArgb(),
            backgroundColor = selectedBgColor.second.toArgb()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text("Select QR Code Color:")
        ColorSelector(qrColors, selectedQrColor) { selectedQrColor = it }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Select Background Color:")
        ColorSelector(bgColors, selectedBgColor) { selectedBgColor = it }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .sizeIn(minHeight = 200.dp, minWidth = 200.dp)
                .border(
                    border = BorderStroke(1.dp, ComposeColor.LightGray),
                    shape = RoundedCornerShape(1.dp)
                )
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(200.dp)
                )

            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = {
            bitmap?.let { shareBitmap(context, it) }
        }) {
            Text("Share QR Code")
        }
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            value = text,
            onValueChange = { text = it },
            label = { Text("Enter any Text") },
        )

    }
}

@Composable
fun ColorSelector(
    colorOptions: List<Pair<String, ComposeColor>>,
    selected: Pair<String, ComposeColor>,
    onSelect: (Pair<String, ComposeColor>) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        colorOptions.forEach { (name, color) ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(
                        border = BorderStroke(1.dp, ComposeColor.LightGray),
                        shape = RoundedCornerShape(1.dp)
                    )
                    .background(color)
                    .clickable { onSelect(name to color) },
                contentAlignment = Alignment.Center
            ) {
                if (selected.first == name) {
                    Text("✓", color = ComposeColor.White)
                }
            }
        }
    }
}

fun generateQrCode(
    text: String,
    size: Int = 512,
    codeColor: Int = AndroidColor.BLACK,
    backgroundColor: Int = AndroidColor.TRANSPARENT
): Bitmap? {
    return try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            size,
            size
        )

        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) codeColor else backgroundColor)
            }
        }

        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


fun shareBitmap(context: Context, bitmap: Bitmap) {
    try {
        // Save bitmap to cache
        val cachePath = File(context.cacheDir, "qr_images")
        cachePath.mkdirs()
        val file = File(cachePath, "shared_qr.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // Get URI
        val qrUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        // Create share intent
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, qrUri)
            type = "image/png"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
