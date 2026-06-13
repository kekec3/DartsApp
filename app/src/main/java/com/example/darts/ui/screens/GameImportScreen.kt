package com.example.darts.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darts.viewModel.GameImportViewModel
import com.example.darts.viewModel.ImportUiEvent
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameImportScreen(
    modifier: Modifier = Modifier,
    viewModel: GameImportViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val scanner = remember {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.processIncomingFileUri(context, it) }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is ImportUiEvent.OnImportCompletedSuccess -> {
                    Toast.makeText(context, "Import successful", Toast.LENGTH_SHORT).show()
                    onBack()
                }
                is ImportUiEvent.ShowToast -> {
                    Toast.makeText(context, event.msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val hasData = uiState.parsedPayloadText != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        TopAppBar(
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Import Match Data",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ─────────────────────────────────────────────
            // INPUT STATE (NO DATA YET)
            // ─────────────────────────────────────────────
            if (!hasData) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.65f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0F0F0F))
                        .border(1.dp, Color.DarkGray, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {

                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color(0xFF76B947),
                            modifier = Modifier.size(56.dp)
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            "Scan or Import Match Data",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Text(
                            "Use QR or file to import .darts match history",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                scanner.startScan()
                                    .addOnSuccessListener { barcode ->
                                        barcode.rawValue?.let {
                                            viewModel.processQrCodeScanResult(it)
                                        }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            context,
                                            "Scan failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF76B947)
                            )
                        ) {
                            Text("Scan QR", color = Color.Black)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Card(
                    onClick = { filePickerLauncher.launch("*/*") },
                    colors = CardDefaults.cardColors(Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = Color(0xFF76B947)
                        )

                        Spacer(Modifier.width(16.dp))

                        Column {
                            Text(
                                "Browse File",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                ".darts or JSON export",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────
            // PREVIEW STATE (DATA LOADED)
            // ─────────────────────────────────────────────
            else {

                Text(
                    "Ready to Import",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {

                        Text(
                            text = uiState.selectedFileName ?: "QR Import",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = uiState.parsedPayloadText
                                ?.take(120)
                                ?: "No preview available",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        TextButton(
                            onClick = { viewModel.clearImportSelection() }
                        ) {
                            Text("Reset", color = Color.Red)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ─────────────────────────────────────────────
            // ACTIONS
            // ─────────────────────────────────────────────
            if (uiState.isProcessing) {
                CircularProgressIndicator(color = Color(0xFF76B947))
                Spacer(Modifier.height(12.dp))
            }

            if (uiState.errorMessage != null) {
                Text(
                    uiState.errorMessage!!,
                    color = Color.Red,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = { viewModel.executeImportConfirmation() },
                enabled = hasData && !uiState.isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF76B947),
                    disabledContainerColor = Color.DarkGray
                )
            ) {
                Text(
                    "CONFIRM IMPORT",
                    color = if (hasData) Color.Black else Color.Gray
                )
            }
        }
    }
}