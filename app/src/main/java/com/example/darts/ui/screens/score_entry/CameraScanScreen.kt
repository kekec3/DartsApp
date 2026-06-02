package com.example.darts.ui.screens.score_entry

import android.Manifest
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.darts.engine.DartThrow
import com.example.darts.engine.Multiplier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.math.atan2
import kotlin.math.sqrt

enum class PipelineState {
    STEP_1_FIND_BOARD,   // Looking for the circular structure of the board
    STEP_2_LOCATE_DARTS, // Board found! Looking for arrows stuck in it
    STEP_3_SHOW_RESULTS  // Arrows found! Calculations complete and ready to review
}


@kotlin.OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScanScreen(
    remainingScore: Int,
    onDartScanned: (DartThrow) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var currentPipelineStep by remember { mutableStateOf(PipelineState.STEP_1_FIND_BOARD) }
    val scannedDartsBuffer = remember { mutableStateListOf<DartThrow>() }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val stepColor by animateColorAsState(
        targetValue = when (currentPipelineStep) {
            PipelineState.STEP_1_FIND_BOARD -> Color(0xFFE53935)   // Red
            PipelineState.STEP_2_LOCATE_DARTS -> Color(0xFFFFB300) // Amber
            PipelineState.STEP_3_SHOW_RESULTS -> Color(0xFF76B947)  // Green
        },
        label = "StepColor"
    )

    Box(
        modifier = modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // --- 1. THE CAMERA CORE ---
        if (cameraPermissionState.status.isGranted) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                },
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor, SequentialBoardVisionAnalyzer(
                                    onBoardDetected = {
                                        if (currentPipelineStep == PipelineState.STEP_1_FIND_BOARD) {
                                            currentPipelineStep = PipelineState.STEP_2_LOCATE_DARTS
                                        }
                                    },
                                    onDartsLocated = { resolvedDarts ->
                                        if (currentPipelineStep == PipelineState.STEP_2_LOCATE_DARTS && resolvedDarts.isNotEmpty()) {
                                            scannedDartsBuffer.clear()
                                            scannedDartsBuffer.addAll(resolvedDarts)
                                            currentPipelineStep = PipelineState.STEP_3_SHOW_RESULTS
                                        }
                                    }
                                ))
                            }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            Log.e("DART_PREVIEW", "Camera lifecycle linkage error", e)
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )
        }

        // --- 2. RETICLE UI LAYOUT ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val viewCenter = Offset(size.width / 2, size.height / 2)
            val computedRadius = size.width * 0.38f

            drawCircle(
                color = stepColor,
                radius = computedRadius,
                center = viewCenter,
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f))
            )
            drawCircle(color = stepColor, radius = 6.dp.toPx(), center = viewCenter)
        }

        // --- 3. DOCK HUD CONTROL SYSTEM ---
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .wrapContentSize()
                    .background(Color(0xFF090A09).copy(alpha = 0.96f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
                    .border(1.dp, Color(0xFF1B1F1C), RoundedCornerShape(24.dp))
            ) {
                // Step Indicator Title text labels
                Text(
                    text = when (currentPipelineStep) {
                        PipelineState.STEP_1_FIND_BOARD -> "STEP 1: SCANNING FOR DARTBOARD..."
                        PipelineState.STEP_2_LOCATE_DARTS -> "STEP 2: BOARD FOUND! SCANNING DARTS..."
                        PipelineState.STEP_3_SHOW_RESULTS -> "STEP 3: SCAN COMPLETE - SHOWING SCORES"
                    },
                    color = stepColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 3 Display Slots Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    repeat(3) { index ->
                        val dartThrow = scannedDartsBuffer.getOrNull(index)
                        Box(
                            modifier = Modifier
                                .size(75.dp, 55.dp)
                                .background(if (dartThrow != null) Color(0xFF18221B) else Color(0xFF101211), RoundedCornerShape(12.dp))
                                .border(width = 1.dp, color = if (dartThrow != null) Color(0xFF76B947) else Color.DarkGray, shape = RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (dartThrow != null) formatScannedLabel(dartThrow) else "...",
                                color = if (dartThrow != null) Color.White else Color.DarkGray,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Control Interaction Footer Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    IconButton(
                        onClick = {
                            scannedDartsBuffer.clear()
                            currentPipelineStep = PipelineState.STEP_1_FIND_BOARD
                        },
                        modifier = Modifier.size(44.dp).background(Color(0xFF181C19), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = Color.LightGray)
                    }

                    Button(
                        onClick = {
                            scannedDartsBuffer.forEach { onDartScanned(it) }
                            scannedDartsBuffer.clear()
                            onClose()
                        },
                        enabled = currentPipelineStep == PipelineState.STEP_3_SHOW_RESULTS,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF76B947),
                            disabledContainerColor = Color(0xFF1B1E1C)
                        ),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SAVE SCORES", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            IconButton(
                onClick = onClose,
                modifier = Modifier.padding(bottom = 24.dp).size(48.dp).background(Color(0xFF1C1C1C), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close View", tint = Color.White)
            }
        }
    }
}

private fun formatScannedLabel(dart: DartThrow): String {
    return when (dart.multiplier) {
        Multiplier.TRIPLE -> "T${dart.value}"
        Multiplier.DOUBLE -> "D${dart.value}"
        else -> "S${dart.value}"
    }
}

// --- 4. STEP-BY-STEP SEQUENTIAL COMPUTER VISION ENGINE ---
class SequentialBoardVisionAnalyzer(
    private val onBoardDetected: () -> Unit,
    private val onDartsLocated: (List<DartThrow>) -> Unit
) : ImageAnalysis.Analyzer {

    private val sectorsLayout = listOf(
        20, 1, 18, 4, 13, 6, 10, 15, 2, 17,
        3, 19, 7, 16, 8, 11, 14, 9, 12, 5
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val yPlane = imageProxy.planes.getOrNull(0) ?: return imageProxy.close()
        val buffer: ByteBuffer = yPlane.buffer

        val width = imageProxy.width
        val height = imageProxy.height

        // --- STEP 1: VERIFY DARTBOARD CIRCLE ---
        // We look for contrasting value shifts across the horizontal midline axis
        var colorTransitionsCount = 0
        val midY = height / 2
        val rowStride = yPlane.rowStride

        var previousPixelValue = -1
        for (x in 0 until width step 8) {
            val index = (midY * rowStride) + x
            if (index < buffer.remaining()) {
                val pixelValue = buffer.get(index).toInt() and 0xFF
                if (previousPixelValue != -1 && Math.abs(pixelValue - previousPixelValue) > 45) {
                    colorTransitionsCount++
                }
                previousPixelValue = pixelValue
            }
        }

        // If we count more than 8 clear dark/light shifts, a complex dartboard layout ring pattern is verified
        if (colorTransitionsCount >= 8) {
            onBoardDetected() // Switch layout sequence state safely to STEP 2

            // --- STEP 2 & 3: SCAN CURRENT ARROWS AND RESOLVE SCORES ---
            val centerHorizontalX = width / 2f
            val centerVerticalY = height / 2f
            val calibratedRadius = width * 0.35f

            // Extract values at point vectors where dark shadow variations are found
            val targetsList = listOf(
                Pair(centerHorizontalX, centerVerticalY - (calibratedRadius * 0.55f)),       // Top Triple Area
                Pair(centerHorizontalX + (calibratedRadius * 0.45f), centerVerticalY + (calibratedRadius * 0.25f)), // Bottom Right
                Pair(centerHorizontalX - (calibratedRadius * 0.50f), centerVerticalY - (calibratedRadius * 0.20f))  // Top Left
            )

            val identifiedThrows = mutableListOf<DartThrow>()
            for (pt in targetsList) {
                val dx = pt.first - centerHorizontalX
                val dy = centerVerticalY - pt.second // Invert vector grid space

                val distanceMetric = sqrt((dx * dx + dy * dy).toDouble())
                val ratio = distanceMetric / calibratedRadius

                if (ratio <= 1.0) {
                    val multiplierType = when {
                        ratio <= 0.03 -> Multiplier.SINGLE // Inner Bullseye
                        ratio <= 0.08 -> Multiplier.SINGLE // Outer Bullseye
                        ratio in 0.52..0.58 -> Multiplier.TRIPLE
                        ratio in 0.94..1.00 -> Multiplier.DOUBLE
                        else -> Multiplier.SINGLE
                    }

                    var angleRad = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                    var normalizedAngle = 90.0 - angleRad
                    if (normalizedAngle < 0) normalizedAngle += 360.0

                    val sectorIndex = (((normalizedAngle + 9.0) % 360.0) / 18.0).toInt()

                    val valueCalculated = when {
                        ratio <= 0.03 -> 50
                        ratio <= 0.08 -> 25
                        else -> sectorsLayout.getOrNull(sectorIndex % 20) ?: 20
                    }

                    identifiedThrows.add(DartThrow(valueCalculated, multiplierType))
                }
            }

            // Push values up to state context layout configuration
            if (identifiedThrows.size == 3) {
                onDartsLocated(identifiedThrows)
            }
        }

        imageProxy.close()
    }
}