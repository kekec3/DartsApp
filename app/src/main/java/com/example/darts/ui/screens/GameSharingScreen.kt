package com.example.darts.ui.screens

import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.darts.viewModel.GameSharingViewModel
import com.example.darts.viewModel.ShareUiEvent
import com.example.darts.viewModel.ShareUiState
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.Hashtable

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSharingScreen(
    modifier: Modifier = Modifier,
    viewModel: GameSharingViewModel,
    onBack: () -> Unit = {},
    onImportData: () -> Unit = {}
) {
    val context = LocalContext.current
    val players by viewModel.players.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val screenState by viewModel.screenState.collectAsState()

    var selectedPlayer by remember { mutableStateOf<Player?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is ShareUiEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                is ShareUiEvent.LaunchSystemIntent -> context.startActivity(event.intent)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopNearbySharing() }
    }

    Column(modifier = modifier.fillMaxSize().background(Color.Black)) {
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

        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                    .clickable { isDropdownExpanded = true }
                    .padding(16.dp)
            ) {
                Text(
                    text = selectedPlayer?.username ?: "Select player",
                    color = if (selectedPlayer != null) Color.White else Color.DarkGray
                )
                DropdownMenu(expanded = isDropdownExpanded, onDismissRequest = { isDropdownExpanded = false }) {
                    players.forEach { player ->
                        DropdownMenuItem(
                            text = { Text(player.username) },
                            onClick = { selectedPlayer = player; isDropdownExpanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            selectedPlayer?.let { player ->
                LaunchedEffect(player) { viewModel.startNearbyAdvertising(context, player) }

                Card(
                    colors = CardDefaults.cardColors(Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(player.username, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        if (screenState.isGeneratingQr) {
                            CircularProgressIndicator(color = Color(0xFF76B947))
                        } else {
                            screenState.qrPayload?.let { token ->
                                QRGeneratorContainer(textToEncode = token)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Share", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                ShareOptionItem("WA File", Icons.Default.Share, Color(0xFF075E54)) {
                    selectedPlayer?.let { viewModel.shareViaApplicationFile(context, it, "com.whatsapp") }
                }
                ShareOptionItem("Gmail", Icons.Default.Email, Color(0xFFEA4335)) {
                    selectedPlayer?.let { viewModel.shareViaApplicationFile(context, it, "com.google.android.gm") }
                }
                ShareOptionItem("More", Icons.Default.MoreVert, Color.Gray) {
                    selectedPlayer?.let { viewModel.shareViaApplicationFile(context, it, null) }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onImportData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = Color(0xFF76B947)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("IMPORT DATA", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { selectedPlayer?.let { viewModel.exportToPublicDownloads(context, it) } },
                enabled = selectedPlayer != null && uiState !is ShareUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFF76B947)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("EXPORT TO DOWNLOADS", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ShareOptionItem(label: String, icon: ImageVector, bgColor: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(bgColor), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
fun QRGeneratorContainer(textToEncode: String) {
    Box(modifier = Modifier.size(160.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
        val qrBitmap = remember(textToEncode) { generateQrCodeBitmap(textToEncode, 512, 512) }
        qrBitmap?.let { Image(painter = BitmapPainter(it.asImageBitmap()), contentDescription = "QR", modifier = Modifier.size(140.dp)) }
    }
}

private fun generateQrCodeBitmap(content: String, width: Int, height: Int): Bitmap? {
    return try {
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, Hashtable<EncodeHintType, Any>().apply { put(EncodeHintType.MARGIN, 1) })
        val pixels = IntArray(width * height) { i -> if (bitMatrix.get(i % width, i / width)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { setPixels(pixels, 0, width, 0, 0, width, height) }
    } catch (e: Exception) { null }
}