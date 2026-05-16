package com.example.darts.ui.screens.score_entry

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darts.engine.DartThrow
import com.example.darts.ui.theme.LimePrimary
import com.example.darts.ui.theme.TextSecondary
import com.example.darts.utils.CommandParser
import com.example.darts.utils.VoiceInputManager
import com.example.darts.utils.VoiceStatus
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceRecognitionScreen(
    onDartAdded: (DartThrow) -> Unit,
    onUndo: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    // Build manager once; rebuild if callbacks change
    val onDartRef  = rememberUpdatedState(onDartAdded)
    val onUndoRef  = rememberUpdatedState(onUndo)
    val onSubmitRef = rememberUpdatedState(onSubmit)

    val manager = remember {
        VoiceInputManager(context) { command ->
            when (command) {
                is CommandParser.VoiceCommand.Throw  -> onDartRef.value(command.dart)
                is CommandParser.VoiceCommand.Undo   -> onUndoRef.value()
                is CommandParser.VoiceCommand.Submit -> onSubmitRef.value()
                else -> {}
            }
        }
    }

    // Start/stop with the composable's lifetime
    DisposableEffect(micPermission.status.isGranted) {
        if (micPermission.status.isGranted) manager.start()
        onDispose { manager.destroy() }
    }

    val state = manager.uiState

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        Spacer(Modifier.height(8.dp))

        // ── Mic button with pulse ring ────────────────────────────────────────
        MicPulseButton(
            status    = state.status,
            hasError  = state.isError,
            hasPermission = micPermission.status.isGranted,
            onTapWhenNoPermission = { micPermission.launchPermissionRequest() }
        )

        // ── Status label ──────────────────────────────────────────────────────
        Text(
            text = when {
                !micPermission.status.isGranted -> "Tap mic to grant permission"
                state.status == VoiceStatus.LISTENING   -> "Listening…"
                state.status == VoiceStatus.PROCESSING  -> "Processing…"
                else                                    -> "Tap to start"
            },
            color      = if (state.isError) Color(0xFFFF5336) else LimePrimary,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // ── Transcript ────────────────────────────────────────────────────────
        if (state.transcript.isNotEmpty()) {
            Text(
                text      = "\"${state.transcript}\"",
                color     = TextSecondary,
                fontSize  = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        // ── Command feedback chip ─────────────────────────────────────────────
        if (state.feedback.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (state.isError)
                    Color(0xFFFF5336).copy(alpha = 0.12f)
                else
                    LimePrimary.copy(alpha = 0.12f)
            ) {
                Text(
                    text      = state.feedback,
                    color     = if (state.isError) Color(0xFFFF5336) else LimePrimary,
                    fontSize  = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier  = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }

        // Error hint
        if (state.errorHint.isNotEmpty()) {
            Text(
                text     = state.errorHint,
                color    = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.weight(1f))

        // ── Command reference ─────────────────────────────────────────────────
        CommandReference()

        Spacer(Modifier.height(8.dp))
    }
}

// ── Pulsing mic ───────────────────────────────────────────────────────────────

@Composable
private fun MicPulseButton(
    status: VoiceStatus,
    hasError: Boolean,
    hasPermission: Boolean,
    onTapWhenNoPermission: () -> Unit
) {
    val isListening = status == VoiceStatus.LISTENING

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue   = 1f,
        targetValue    = 1.18f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(700, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val ringColor = when {
        !hasPermission -> TextSecondary
        hasError       -> Color(0xFFFF5336)
        isListening    -> LimePrimary
        else           -> LimePrimary.copy(alpha = 0.4f)
    }

    Box(contentAlignment = Alignment.Center) {
        // Outer pulse ring (only when listening)
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulse)
                    .border(2.dp, ringColor.copy(alpha = 0.35f), CircleShape)
            )
        }
        // Inner button
        Surface(
            modifier  = Modifier.size(96.dp),
            shape     = CircleShape,
            color     = if (isListening) LimePrimary.copy(alpha = 0.15f) else Color(0xFF1A201B),
            onClick   = if (!hasPermission) onTapWhenNoPermission else ({})
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector        = if (hasPermission) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Microphone",
                    tint               = ringColor,
                    modifier           = Modifier.size(48.dp)
                )
            }
        }
    }
}

// ── Command cheat-sheet ───────────────────────────────────────────────────────

@Composable
private fun CommandReference() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1A201B)
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ReferenceRow("\"Twenty\"",        "→  20")
            ReferenceRow("\"Double sixteen\"","→  D16")
            ReferenceRow("\"Triple twenty\"", "→  T20  (60)")
            ReferenceRow("\"Bull\"",          "→  25")
            ReferenceRow("\"Double bull\"",   "→  50")
            ReferenceRow("\"Miss\"",          "→  0")
            ReferenceRow("\"Undo\"",          "→  remove last dart")
            ReferenceRow("\"Submit\"",        "→  end turn")
        }
    }
}

@Composable
private fun ReferenceRow(command: String, result: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(command, color = Color.White,   fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(result,  color = LimePrimary,   fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}