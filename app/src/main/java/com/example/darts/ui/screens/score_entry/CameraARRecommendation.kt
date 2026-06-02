package com.example.darts.ui.screens.score_entry

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraArRecommendationScreen(
    remainingScore: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // --- 1. SMART CHECKOUT ROUTING SYSTEM ---
    val (targetText, targetAngleDegrees, isDouble, isTriple) = remember(remainingScore) {
        when {
            // High scores: Setup strategy toward optimal checkouts via standard tracks
            remainingScore > 60 -> {
                val remainder = remainingScore % 20
                if (remainder == 0 || remainder % 2 == 0) {
                    quadruple("T20", getAngleForSector(20), false, true)
                } else {
                    quadruple("T19", getAngleForSector(19), false, true)
                }
            }

            // Direct boundary targets
            remainingScore == 60 -> quadruple("T20", getAngleForSector(20), false, true)
            remainingScore == 50 -> quadruple("BULLSEYE", 0f, false, false) // Central center check handled explicitly

            // Setup Single beds down to Master Double D20 (Clean 40 remaining)
            remainingScore in 41..59 -> {
                val singleSetupTarget = remainingScore - 40
                quadruple("S$singleSetupTarget", getAngleForSector(singleSetupTarget), false, false)
            }

            // High Priority Checkout Doubles
            remainingScore == 40 -> quadruple("D20", getAngleForSector(20), true, false)
            remainingScore == 36 -> quadruple("D18", getAngleForSector(18), true, false)
            remainingScore == 32 -> quadruple("D16", getAngleForSector(16), true, false)
            remainingScore == 24 -> quadruple("D12", getAngleForSector(12), true, false)
            remainingScore == 16 -> quadruple("D8", getAngleForSector(8), true, false)

            // Direct Clean Evens Checkouts
            remainingScore % 2 == 0 -> {
                val targetedHalfValue = remainingScore / 2
                quadruple("D$targetedHalfValue", getAngleForSector(targetedHalfValue), true, false)
            }

            // Emergency Fallback: Break odd leftovers with simple Single 1 marker to leave clean even sets
            else -> quadruple("S1", getAngleForSector(1), false, false)
        }
    }

    // --- 2. PULSATING HUD AR ANIMATION MATRIX ---
    val infiniteTransition = rememberInfiniteTransition(label = "NeonArPulse")
    val pulseScaleMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(
        modifier = modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // --- 3. HARDWARE CAPTURE GRAPHICS FRAMEWORK ---
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
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("ALLOW CAMERA FOR AR HUDS", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- 4. DIGITAL AR OVERLAY OVER THE CAMERA ENVIRONMENT ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hX = size.width / 2f
            val vY = size.height / 2f
            val physicalBoardReferenceRadius = size.width * 0.39f

            // Static Target Calibration Guide Ring
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = physicalBoardReferenceRadius,
                center = Offset(hX, vY),
                style = Stroke(width = 2.dp.toPx())
            )

            // Calculate precise geometric placement positions across the polar field array
            if (targetText == "BULLSEYE") {
                // Bullseye tracking is isolated at the exact absolute center intercept coordinate
                drawCircle(color = Color(0x4500FFCC), radius = 30.dp.toPx() * pulseScaleMultiplier, center = Offset(hX, vY))
                drawCircle(color = Color(0xFF00E5FF), radius = 8.dp.toPx(), center = Offset(hX, vY))
            } else {
                val rad = Math.toRadians(targetAngleDegrees.toDouble())
                val layoutDepthRatio = when {
                    isDouble -> 0.96f // Outer Track Perimeter boundary loop
                    isTriple -> 0.58f // Inner Track high value multiplier loop
                    else -> 0.77f     // Standard Single Bed baseline middle point
                }

                val linearOffsetDistance = physicalBoardReferenceRadius * layoutDepthRatio
                val targetCoordinateX = hX + (linearOffsetDistance * cos(rad)).toFloat()
                val targetCoordinateY = vY + (linearOffsetDistance * sin(rad)).toFloat()

                // Neon Translucent Target Overlay Ring
                drawCircle(
                    color = Color(0x4500FFCC),
                    radius = 24.dp.toPx() * pulseScaleMultiplier,
                    center = Offset(targetCoordinateX, targetCoordinateY)
                )
                // Crisp Concentrated Intersection Point Center Anchor
                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = 7.dp.toPx(),
                    center = Offset(targetCoordinateX, targetCoordinateY)
                )
            }
        }

        // --- 5. INTERFACE HEADS-UP DISPLAY CONTROL CONTEXT CARD ---
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F0D).copy(alpha = 0.85f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(top = 16.dp).border(1.dp, Color(0xFF1F2925), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AUGMENTED RECOMMENDATION ACQUIRED",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "AIM AT: $targetText",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xFF141414).copy(alpha = 0.7f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Exit Target HUD View", tint = Color.White)
            }
        }
    }
}

// --- 6. GLOBAL MATHEMATICAL VECTOR GEOMETRY CONVERTER ---
private fun getAngleForSector(sector: Int): Float {
    val clockwiseLayoutSectors = listOf(
        20, 1, 18, 4, 13, 6, 10, 15, 2, 17,
        3, 19, 7, 16, 8, 11, 14, 9, 12, 5
    )
    val numericalIndex = clockwiseLayoutSectors.indexOf(sector)
    if (numericalIndex == -1) return -90f // Default fallback vector index straight vertical setup axis

    // Each slice occupies exactly 18 degrees out of 360 around the board circumference axis.
    // We adjust standard angles by subtracting 90 degrees because 0 degrees starts at
    // 3 o'clock in standard math circles, but 20 lives directly at 12 o'clock.
    return (numericalIndex * 18f) - 90f
}

private fun quadruple(text: String, angle: Float, isD: Boolean, isT: Boolean) = Quadruple(text, angle, isD, isT)
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)