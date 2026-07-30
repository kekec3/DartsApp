package com.example.darts.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.darts.viewModel.GameImportViewModel
import com.example.darts.viewModel.ImportUiEvent
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameImportScreen(
    modifier: Modifier = Modifier,
    viewModel: GameImportViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var showScanner by remember { mutableStateOf(false) }

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.processIncomingFileUri(context, it) }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { event ->
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

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {

        Column(
            modifier = Modifier.fillMaxSize()
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

                if (!hasData) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.65f)
                            .background(Color(0xFF0F0F0F), RoundedCornerShape(24.dp))
                            .border(1.dp, Color.DarkGray, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
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
                                fontWeight = FontWeight.Bold
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
                                    if (ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    showScanner = true },
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
                                Text("Browse File", color = Color.White, fontWeight = FontWeight.Bold)
                                Text(".darts or JSON export", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                } else {

                    Text("Ready to Import", color = Color.White, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(Color(0xFF1A1A1A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                uiState.selectedFileName ?: "QR Import",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                uiState.parsedPayloadText?.take(120) ?: "",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )

                            Spacer(Modifier.height(12.dp))

                            TextButton(onClick = { viewModel.clearImportSelection() }) {
                                Text("Reset", color = Color.Red)
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                if (uiState.isProcessing) {
                    CircularProgressIndicator(color = Color(0xFF76B947))
                    Spacer(Modifier.height(12.dp))
                }

                if (uiState.errorMessage != null) {
                    Text(uiState.errorMessage!!, color = Color.Red)
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
                    Text("CONFIRM IMPORT", color = Color.Black)
                }
            }
        }

        if (showScanner) {
            QrCameraScanner(
                onResult = { scannedToken ->
                    showScanner = false
                    // This now triggers Nearby Discovery instead of the old HTTP logic
                    viewModel.processQrCodeScanResult(context, scannedToken)
                },
                onClose = { showScanner = false }
            )
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun QrCameraScanner(
    onResult: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // A QR code sits in front of the lens for many frames, so the analyzer fires
    // repeatedly for the same code. Deliver only the first hit: a second delivery
    // restarts Nearby discovery and fails with STATUS_ALREADY_DISCOVERED (8002).
    val hasDelivered = remember { AtomicBoolean(false) }

    // The camera is bound to the screen lifecycle, so it keeps analysing frames after
    // this composable leaves composition unless we unbind it explicitly.
    val boundProvider = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val boundScanner = remember { mutableStateOf<BarcodeScanner?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            boundProvider.value?.unbindAll()
            boundScanner.value?.close()
            boundProvider.value = null
            boundScanner.value = null
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->

            val previewView = PreviewView(ctx)

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({

                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                preview.setSurfaceProvider(previewView.surfaceProvider)

                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()

                val scanner = BarcodeScanning.getClient(options)

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(
                    ContextCompat.getMainExecutor(ctx)
                ) { imageProxy ->

                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {

                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val value = barcodes.firstOrNull()?.rawValue
                                if (!value.isNullOrEmpty() && hasDelivered.compareAndSet(false, true)) {
                                    onResult(value)
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        analysis
                    )
                    boundProvider.value = cameraProvider
                    boundScanner.value = scanner
                } catch (e: Exception) {
                    Log.e("QR", "Camera bind failed", e)
                    scanner.close()
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}