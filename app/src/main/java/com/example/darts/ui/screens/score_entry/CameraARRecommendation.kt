package com.example.darts.ui.screens.score_entry

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.darts.engine.DartRecommendationEngine
import com.example.darts.engine.DartTarget
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

    val checkoutPath = remember(remainingScore) {
        DartRecommendationEngine.getBestCheckout(remainingScore)
    }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    val transition = rememberInfiniteTransition(label = "ar_hud_cinematics")
    val masterTimeline by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "timeline"
    )

    val sonarRadarRadius by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearOutSlowInEasing)),
        label = "radar"
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (cameraPermissionState.status.isGranted) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                update = { view ->
                    val providerFuture = ProcessCameraProvider.getInstance(context)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(view.surfaceProvider)
                        }
                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f - 60f)
            val R = size.width * 0.44f

            val bInner = R * 0.04f
            val bOuter = R * 0.09f
            val tInner = R * 0.54f
            val tOuter = R * 0.64f
            val dInner = R * 0.91f
            val dOuter = R * 1.00f

            // 1. Grid Overlay Background
            drawHighVisHoloGrid(center, bInner, bOuter, tInner, tOuter, dInner, dOuter)

            // 2. Pulse Sweep Radar Effect
            drawCircle(
                color = Color(0xFF00FFCC).copy(alpha = (1f - sonarRadarRadius) * 0.4f),
                radius = dOuter * 1.2f * sonarRadarRadius,
                center = center,
                style = Stroke(width = 3f)
            )

            val darts = checkoutPath?.darts ?: emptyList()

            val activeDartIndex = masterTimeline.toInt().coerceIn(0, maxOf(0, darts.lastIndex))
            val currentStageTimeline = masterTimeline - activeDartIndex

            val lockOnProgress = (currentStageTimeline / 0.25f).coerceIn(0f, 1f)
            val flightProgress = ((currentStageTimeline - 0.25f) / 0.45f).coerceIn(0f, 1f)
            val impactProgress = ((currentStageTimeline - 0.70f) / 0.30f).coerceIn(0f, 1f)

            darts.forEachIndexed { index, dartTarget ->
                val isTargetActiveNow = index == activeDartIndex
                val hasBeenThrown = index < activeDartIndex

                val targetColor = if (isTargetActiveNow) Color(0xFF00FFCC) else Color(0xFF00FF66).copy(alpha = 0.45f)
                val currentLockProgress = if (isTargetActiveNow) lockOnProgress else 1.0f

                val targetInnerR: Float
                val targetOuterR: Float
                when {
                    dartTarget.name == "BULL" -> { targetInnerR = 0f; targetOuterR = bInner }
                    dartTarget.name == "OUTER_BULL" -> { targetInnerR = bInner; targetOuterR = bOuter }
                    dartTarget.isTriple -> { targetInnerR = tInner; targetOuterR = tOuter }
                    dartTarget.isDouble -> { targetInnerR = dInner; targetOuterR = dOuter }
                    else -> { targetInnerR = tOuter; targetOuterR = dInner }
                }

                val startAngle = dartTarget.angle - 9f

                // Render Vertical 3D Solid Target Blocks (Kept completely untouched)
                if (dartTarget.name == "BULL") {
                    drawAR3DCircle(center, targetOuterR, targetColor, currentLockProgress)
                } else {
                    drawAR3DBlock(center, startAngle, 18f, targetInnerR, targetOuterR, targetColor, currentLockProgress)
                }

                // Calculate targeted intersection coordinate points
                val targetDist = (targetInnerR + targetOuterR) / 2f
                val rad = Math.toRadians(dartTarget.angle.toDouble())
                val targetHit = if (dartTarget.name == "BULL") center else
                    center + Offset((targetDist * cos(rad)).toFloat(), (targetDist * sin(rad)).toFloat())

                drawLockOnBrackets(targetHit, currentLockProgress, isTargetActiveNow)

                // 3. Floating Yellow 3D Multiplier (Positioned directly ABOVE the block, not over-stretched)
                if (dartTarget.isDouble || dartTarget.isTriple) {
                    val labelText = if (dartTarget.isTriple) "x3" else "x2"
                    val block3DHeightOffset = currentLockProgress * -25f

                    // Added -55f extra buffer so it sits perfectly in free space above the block height
                    val multiplierPos = targetHit + Offset(0f, block3DHeightOffset - 55f)

                    drawHighVisYellowMultiplier(textMeasurer, labelText, multiplierPos, currentLockProgress)
                }

                // 4. Thickened Laser Vector Flight Tracking Architecture
                val handOrigin = Offset(size.width / 2f, size.height + 150f)

                if (isTargetActiveNow && currentStageTimeline >= 0.25f) {
                    val currentX = androidx.compose.ui.util.lerp(handOrigin.x, targetHit.x, flightProgress)
                    val linearY = androidx.compose.ui.util.lerp(handOrigin.y, targetHit.y, flightProgress)
                    val arcHeight = 400f
                    val currentY = linearY - (sin(flightProgress * Math.PI).toFloat() * arcHeight)
                    val currentPos = Offset(currentX, currentY)

                    if (flightProgress > 0.0f) {
                        // Outer neon aura trail
                        drawLine(
                            color = targetColor.copy(alpha = 0.25f * (1f - impactProgress)),
                            start = handOrigin,
                            end = currentPos,
                            strokeWidth = 24f,
                            cap = StrokeCap.Round
                        )
                        // Medium tracking core beam
                        drawLine(
                            color = targetColor.copy(alpha = 0.6f * (1f - impactProgress)),
                            start = handOrigin,
                            end = currentPos,
                            strokeWidth = 12f,
                            cap = StrokeCap.Round
                        )
                        // High intensity center line
                        drawLine(
                            color = Color.White.copy(alpha = 0.9f * (1f - impactProgress)),
                            start = handOrigin,
                            end = currentPos,
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )

                        // Head tip tracking point indicators
                        drawCircle(
                            color = Color.White,
                            radius = 8f,
                            center = currentPos
                        )
                        drawCircle(
                            color = targetColor,
                            radius = 16f,
                            center = currentPos,
                            style = Stroke(width = 3f)
                        )
                    }

                    drawFloatingTelemetry(textMeasurer, targetHit, dartTarget.name, index + 1, currentStageTimeline, impactProgress)
                } else if (hasBeenThrown) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = handOrigin,
                        end = targetHit,
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = Color(0xFF00FF66).copy(alpha = 0.6f),
                        radius = 6f,
                        center = targetHit
                    )
                }
            }

            // 5. Global Diagnostics HUD Feed Overlay
            drawGlobalHUDSystemPanel(textMeasurer, darts, activeDartIndex, currentStageTimeline)
        }

        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            Icon(Icons.Default.Close, null, tint = Color.White)
        }
    }
}

private fun DrawScope.drawHighVisHoloGrid(
    center: Offset, bInner: Float, bOuter: Float,
    tInner: Float, tOuter: Float, dInner: Float, dOuter: Float
) {
    val matrixGreen = Color(0xFF00FF66).copy(alpha = 0.35f)
    drawCircle(matrixGreen, bInner, center, style = Stroke(width = 2.0f))
    drawCircle(matrixGreen, bOuter, center, style = Stroke(width = 3.0f))
    drawCircle(matrixGreen, tInner, center, style = Stroke(width = 2.5f))
    drawCircle(matrixGreen, tOuter, center, style = Stroke(width = 2.5f))
    drawCircle(matrixGreen, dInner, center, style = Stroke(width = 3.0f))
    drawCircle(matrixGreen, dOuter, center, style = Stroke(width = 4.0f))

    for (i in 0 until 20) {
        val rad = Math.toRadians(i * 18.0 - 99.0)
        val start = center + Offset((bOuter * cos(rad)).toFloat(), (bOuter * sin(rad)).toFloat())
        val end = center + Offset((dOuter * cos(rad)).toFloat(), (dOuter * sin(rad)).toFloat())
        drawLine(matrixGreen, start, end, strokeWidth = 2.0f)
    }
}

private fun DrawScope.drawAR3DBlock(
    center: Offset, startAngle: Float, sweep: Float,
    innerR: Float, outerR: Float, color: Color, lockProgress: Float
) {
    val path = Path().apply {
        arcTo(Rect(center.x - outerR, center.y - outerR, center.x + outerR, center.y + outerR), startAngle, sweep, true)
        arcTo(Rect(center.x - innerR, center.y - innerR, center.x + innerR, center.y + innerR), startAngle + sweep, -sweep, false)
        close()
    }

    val verticalLayers = 25
    val totalHeightOffset = lockProgress * -25f

    for (i in 0 until verticalLayers) {
        val stepRatio = i / verticalLayers.toFloat()
        val currentYOffset = stepRatio * totalHeightOffset

        val layerShading = color.copy(alpha = 0.85f).compositeOver(
            Color.Black.copy(alpha = androidx.compose.ui.util.lerp(0.90f, 0.05f, stepRatio))
        )
        withTransform({
            translate(0f, currentYOffset)
        }) {
            drawPath(path, layerShading)
        }
    }

    withTransform({
        translate(0f, totalHeightOffset)
    }) {
        drawPath(path, color.copy(alpha = 0.35f))
        drawPath(path, Color.White.copy(0.9f), style = Stroke(2.0f))
    }
}

private fun DrawScope.drawAR3DCircle(center: Offset, radius: Float, color: Color, lockProgress: Float) {
    val verticalLayers = 25
    val totalHeightOffset = lockProgress * -25f

    for (i in 0 until verticalLayers) {
        val stepRatio = i / verticalLayers.toFloat()
        val currentYOffset = stepRatio * totalHeightOffset

        val layerShading = color.copy(alpha = 0.85f).compositeOver(
            Color.Black.copy(alpha = androidx.compose.ui.util.lerp(0.90f, 0.05f, stepRatio))
        )
        withTransform({
            translate(0f, currentYOffset)
        }) {
            drawCircle(layerShading, radius, center)
        }
    }
    withTransform({
        translate(0f, totalHeightOffset)
    }) {
        drawCircle(color.copy(alpha = 0.35f), radius, center)
        drawCircle(Color.White.copy(0.9f), radius, center, style = Stroke(2.0f))
    }
}

private fun DrawScope.drawLockOnBrackets(target: Offset, lockProgress: Float, isPrimary: Boolean) {
    val size = (if (isPrimary) 55f * (2f - lockProgress) else 40f)
    val alpha = lockProgress * (if (isPrimary) 1.0f else 0.6f)
    val strokeWidth = if (isPrimary) 4.5f else 2.5f
    val c = (if (isPrimary) Color(0xFF00FFCC) else Color(0xFF00FF66)).copy(alpha = alpha)

    drawLine(c, target + Offset(-size, -size), target + Offset(-size + 15f, -size), strokeWidth)
    drawLine(c, target + Offset(-size, -size), target + Offset(-size, -size + 15f), strokeWidth)
    drawLine(c, target + Offset(size, -size), target + Offset(size - 15f, -size), strokeWidth)
    drawLine(c, target + Offset(size, -size), target + Offset(size, -size + 15f), strokeWidth)
    drawLine(c, target + Offset(-size, size), target + Offset(-size + 15f, size), strokeWidth)
    drawLine(c, target + Offset(-size, size), target + Offset(-size, size - 15f), strokeWidth)
    drawLine(c, target + Offset(size, size), target + Offset(size - 15f, size), strokeWidth)
    drawLine(c, target + Offset(size, size), target + Offset(size, size - 15f), strokeWidth)
}

// Custom 3D text renderer setup specifically with a tight extrusion mesh path for visibility clarity
private fun DrawScope.drawHighVisYellowMultiplier(
    measurer: TextMeasurer,
    text: String,
    position: Offset,
    progress: Float
) {
    val textStyle = TextStyle(
        color = Color(0xFFFFEA00), // Pure cyber neon yellow
        fontSize = 29.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
    )

    val textLayoutResult = measurer.measure(text, textStyle)
    val textWidth = textLayoutResult.size.width.toFloat()
    val textHeight = textLayoutResult.size.height.toFloat()
    val textTopLeft = position - Offset(textWidth / 2f, textHeight / 2f)

    // Reduced total steps to 6 to compress the extrusion length and keep it looking balanced
    val tight3DDepthLayers = 6
    for (i in tight3DDepthLayers downTo 1) {
        val depthRatio = i / tight3DDepthLayers.toFloat()
        // Tighter multiplier factor (8f) means layers stay neatly bunched right under the text cap
        val yOffset = depthRatio * 8f * progress

        val depthColorShading = Color(0xFFFFEA00).copy(alpha = 0.85f * progress).compositeOver(
            Color.Black.copy(alpha = androidx.compose.ui.util.lerp(0.90f, 0.35f, depthRatio))
        )

        withTransform({
            translate(0f, yOffset)
        }) {
            drawText(measurer, text, textTopLeft, style = textStyle.copy(color = depthColorShading))
        }
    }

    // Luminous white/yellow cap layer
    withTransform({
        translate(0f, 0f)
    }) {
        drawText(measurer, text, textTopLeft, style = textStyle.copy(color = Color.White.copy(alpha = progress)))
    }
}

private fun DrawScope.drawFloatingTelemetry(
    measurer: TextMeasurer, target: Offset,
    label: String, dartNum: Int, stageTime: Float, impactProgress: Float
) {
    if (stageTime < 0.20f) return
    val textStyle = TextStyle(color = Color(0xFF00FFCC), fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    val boxOffset = target + Offset(60f, -80f)
    val accuracyMetric = if (stageTime >= 0.70f) "LOCKED" else "${(85f + (impactProgress * 14.8f)).coerceAtMost(99.8f)}%"

    drawRoundRect(color = Color.Black.copy(0.7f), topLeft = boxOffset, size = Size(150f, 60f), cornerRadius = CornerRadius(4f))
    drawRoundRect(color = Color(0xFF00FFCC).copy(0.4f), topLeft = boxOffset, size = Size(150f, 60f), style = Stroke(1f), cornerRadius = CornerRadius(4f))

    drawText(measurer, "DART $dartNum: $label", boxOffset + Offset(10f, 6f), style = textStyle.copy(color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
    drawText(measurer, "TRACKING: $accuracyMetric", boxOffset + Offset(10f, 24f), style = textStyle)
    drawLine(Color(0xFF00FFCC).copy(0.5f), target, boxOffset + Offset(0f, 30f), 1f)
}

private fun DrawScope.drawGlobalHUDSystemPanel(
    measurer: TextMeasurer, darts: List<DartTarget>, activeIdx: Int, stageTime: Float
) {
    val textStyle = TextStyle(color = Color(0xFF00FFCC), fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)

    val statusText = when {
        stageTime < 0.25f -> "SYSTEM: LOCKING STEP ${activeIdx + 1}..."
        stageTime < 0.70f -> "SYSTEM: BALLISTIC VECTOR ACTIVE [${activeIdx + 1}/3]"
        else -> "SYSTEM: TARGET ${activeIdx + 1} ENGAGED"
    }

    drawText(measurer, statusText, Offset(40f, 60f), style = textStyle.copy(fontSize = 13.sp, color = Color.White))
    drawText(measurer, "MATH AR ENGINE OVERLAY V3.5 // SEQUENCED MODE", Offset(40f, 90f), style = textStyle.copy(color = Color.White.copy(0.4f)))

    val panelTopLeft = Offset(40f, 130f)
    drawRoundRect(color = Color.Black.copy(alpha = 0.6f), topLeft = panelTopLeft, size = Size(260f, 100f), cornerRadius = CornerRadius(6f))
    drawRoundRect(color = Color(0xFF00FF66).copy(alpha = 0.25f), topLeft = panelTopLeft, size = Size(260f, 100f), style = Stroke(1.5f), cornerRadius = CornerRadius(6f))

    drawText(measurer, "SEQUENCED CHECKOUT PATH:", panelTopLeft + Offset(12f, 10f), style = textStyle.copy(color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp))

    if (darts.isEmpty()) {
        drawText(measurer, "NO CHECKOUT ROUTE FOUND", panelTopLeft + Offset(12f, 35f), style = textStyle.copy(color = Color.Red))
    } else {
        darts.forEachIndexed { idx, dart ->
            val stepYOffset = 32f + (idx * 20f)
            val isCurrent = idx == activeIdx
            val stepColor = if (isCurrent) Color(0xFF00FFCC) else if (idx < activeIdx) Color.White.copy(0.5f) else Color.White
            val prefix = if (isCurrent) "► DART ${idx + 1}:" else "  DART ${idx + 1}:"

            drawText(
                textMeasurer = measurer,
                text = "$prefix ${dart.name} (${dart.score} pts)",
                topLeft = panelTopLeft + Offset(12f, stepYOffset),
                style = textStyle.copy(color = stepColor)
            )
        }
    }
}