package com.example.darts.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameImportScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onImportConfirmed: (String) -> Unit
) {
    val context = LocalContext.current

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var isQrImportDetected by remember { mutableStateOf(false) }
    var parsedPayloadText by remember { mutableStateOf<String?>(null) }

    val scanner = remember {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            isQrImportDetected = false

            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    selectedFileName = cursor.getString(nameIndex)
                }
            }

            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        parsedPayloadText = reader.readText()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file payload data", Toast.LENGTH_SHORT).show()
            }
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
                    Text("Import Match Data", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
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
                .weight(1f)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedFileUri == null && !isQrImportDetected) {

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
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scanner",
                            tint = Color(0xFF76B947),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Scan QR Sharing Code",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Instantly scan your opponent's QR generation screen layout over-the-air to synchronize match files.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                scanner.startScan()
                                    .addOnSuccessListener { barcode ->
                                        barcode.rawValue?.let { qrValue ->
                                            parsedPayloadText = qrValue
                                            isQrImportDetected = true
                                            selectedFileUri = null
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Scan cancelled or failed", Toast.LENGTH_SHORT).show()
                                    }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76B947))
                        ) {
                            Text("Open Live Camera Scanner", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("OR ALTERNATIVELY", color = Color.DarkGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    onClick = { filePickerLauncher.launch("*/*") },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF262626), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color(0xFF76B947))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Browse Device Files", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Select a downloaded .darts record", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }

            } else {
                Text(
                    text = "Match Preview Detected",
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        if (isQrImportDetected) {
                            Text("Source: Wireless QR Dynamic Sync", color = Color(0xFF76B947), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Payload Ready", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Compressed configuration context extracted successfully.", color = Color.Gray, fontSize = 13.sp)
                        } else {
                            Text("Source: Local File System Storage", color = Color(0xFF007AFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(selectedFileName ?: "Imported File", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("URI: ${selectedFileUri.toString().take(45)}...", color = Color.Gray, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = {
                                selectedFileUri = null
                                selectedFileName = null
                                isQrImportDetected = false
                                parsedPayloadText = null
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Clear and Reset Scanner", color = Color.Red, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    parsedPayloadText?.let { onImportConfirmed(it) }
                },
                enabled = parsedPayloadText != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF76B947),
                    disabledConttainerColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "CONFIRM IMPORT",
                    color = if (parsedPayloadText != null) Color.Black else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}