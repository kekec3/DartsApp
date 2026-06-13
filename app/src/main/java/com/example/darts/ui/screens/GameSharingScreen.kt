package com.example.darts.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darts.db.entities.Player
import com.example.darts.ui.viewmodels.GameSharingViewModel
import com.example.darts.ui.viewmodels.ShareUiState
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.Hashtable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSharingScreen(
    modifier: Modifier = Modifier,
    viewModel: GameSharingViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val players by viewModel.players.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var selectedPlayer by remember { mutableStateOf<Player?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val launchShareIntent = { uri: android.net.Uri, text: String, targetPackage: String? ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            targetPackage?.let { setPackage(it) }
        }
        if (targetPackage != null) {
            context.startActivity(intent)
        } else {
            context.startActivity(Intent.createChooser(intent, "Share Darts History"))
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ShareUiState.Success) {
            val successState = uiState as ShareUiState.Success
            if (successState.isSavedToDisk) {
                Toast.makeText(
                    context,
                    "Successfully downloaded match history to Downloads folder!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                launchShareIntent(successState.fileUri, successState.shareText, successState.targetPackage)
            }
            viewModel.resetUiState()
        } else if (uiState is ShareUiState.Error) {
            Toast.makeText(context, (uiState as ShareUiState.Error).message, Toast.LENGTH_LONG).show()
            viewModel.resetUiState()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopAppBar(
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Export Player History", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text("Select Player to Export", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                    .clickable { isDropdownExpanded = true }
                    .padding(16.dp)
            ) {
                Text(
                    text = selectedPlayer?.username ?: "Tap to choose a player...",
                    color = if (selectedPlayer != null) Color.White else Color.DarkGray,
                    fontSize = 16.sp
                )

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.background(Color(0xFF1A1A1A))
                ) {
                    players.forEach { player ->
                        DropdownMenuItem(
                            text = { Text(player.username, color = Color.White) },
                            onClick = {
                                selectedPlayer = player
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            selectedPlayer?.let { player ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(player.username, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(".darts Export Format", color = Color(0xFF76B947), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Scan to Import Instantly", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        QRGeneratorContainer(textToEncode = "https://example.com/darts/import?playerId=${player.idPlayer}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Share via Link & File", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShareOptionItem("WA Link", Icons.Default.Send, Color(0xFF25D366)) {
                    selectedPlayer?.let {
                        val linkText = "Check out my darts match history! Click here to import it: " +
                                "https://example.com/darts/import?playerId=${it.idPlayer}"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, linkText)
                            setPackage("com.whatsapp")
                        }
                        context.startActivity(intent)
                    }
                }

                ShareOptionItem("WA File", Icons.Default.Share, Color(0xFF075E54)) {
                    selectedPlayer?.let { viewModel.prepareExport(context, it, "com.whatsapp") }
                }

                ShareOptionItem("Gmail", Icons.Default.Email, Color(0xFFEA4335)) {
                    selectedPlayer?.let { viewModel.prepareExport(context, it, "com.google.android.gm") }
                }

                ShareOptionItem("More", Icons.Default.MoreVert, Color(0xFF1E1E1E)) {
                    selectedPlayer?.let { viewModel.prepareExport(context, it, null) }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (uiState is ShareUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Color(0xFF76B947))
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = { selectedPlayer?.let { viewModel.exportToPublicDownloads(context, it) } },
                enabled = selectedPlayer != null && uiState !is ShareUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF76B947),
                    disabledContainerColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "PREPARE AND DOWNLOAD TO STORAGE",
                    color = if (selectedPlayer != null) Color.Black else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun ShareOptionItem(label: String, icon: ImageVector, bgColor: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
fun QRGeneratorContainer(textToEncode: String) {
    Box(
        modifier = Modifier
            .size(160.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        QRVisualizerWidget(text = textToEncode)
    }
}

@Composable
fun QRVisualizerWidget(text: String, modifier: Modifier = Modifier) {
    val qrBitmap = remember(text) {
        generateQrCodeBitmap(text, 512, 512)
    }

    qrBitmap?.let { bitmap ->
        Image(
            painter = BitmapPainter(bitmap.asImageBitmap()),
            contentDescription = "QR Code Deep Link",
            modifier = modifier.size(140.dp)
        )
    }
}

private fun generateQrCodeBitmap(content: String, width: Int, height: Int): Bitmap? {
    return try {
        val hints = Hashtable<EncodeHintType, Any>().apply {
            put(EncodeHintType.MARGIN, 1)
        }

        val bitMatrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            width,
            height,
            hints
        )

        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }

        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}