package com.example.darts.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.darts.db.entities.MomentType
import kotlinx.coroutines.delay
import java.io.File

enum class MomentTab { PHOTO, AUDIO, EMOJI }

@Composable
fun MomentScreen(
    onMomentCaptured: (type: MomentType, value: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(MomentTab.PHOTO) }

    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFF0B0F0C)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("CAPTURE MATCH MOMENT", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
        }

        PrimaryTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = Color(0xFF161B17),
            contentColor = Color(0xFF76B947),
            modifier = Modifier.padding(vertical = 16.dp).clip(RoundedCornerShape(8.dp))
        ) {
            Tab(selected = selectedTab == MomentTab.PHOTO, onClick = { selectedTab = MomentTab.PHOTO }, text = { Text("Photo") }, icon = { Icon(Icons.Default.CameraAlt, null) })
            Tab(selected = selectedTab == MomentTab.AUDIO, onClick = { selectedTab = MomentTab.AUDIO }, text = { Text("Audio") }, icon = { Icon(Icons.Default.Mic, null) })
            Tab(selected = selectedTab == MomentTab.EMOJI, onClick = { selectedTab = MomentTab.EMOJI }, text = { Text("Emoji") }, icon = { Icon(Icons.Default.EmojiEmotions, null) })
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            AnimatedContent(targetState = selectedTab, transitionSpec = { fadeIn() togetherWith fadeOut() }) { targetTab ->
                when (targetTab) {
                    MomentTab.PHOTO -> PhotoCaptureView(onPhotoSaved = { path -> onMomentCaptured(MomentType.PHOTO, path) })
                    MomentTab.AUDIO -> AudioRecorderView(onAudioSaved = { path -> onMomentCaptured(MomentType.AUDIO, path) })
                    MomentTab.EMOJI -> EmojiSelectorView(onEmojiSelected = { emojiSymbol -> onMomentCaptured(MomentType.EMOJI, emojiSymbol) })
                }
            }
        }
    }
}

@Composable
fun PhotoCaptureView(onPhotoSaved: (String) -> Unit) {
    val context = LocalContext.current
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap

            // --- FIX: Actually save the bitmap bytes to disk ---
            val filename = "captured_at_${System.currentTimeMillis()}.jpg"
            try {
                // Open a private file output stream inside the app's internal sandbox
                context.openFileOutput(filename, Context.MODE_PRIVATE).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }

                // Retrieve the genuine absolute path to store in Room
                val absolutePath = File(context.filesDir, filename).absolutePath
                onPhotoSaved(absolutePath)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to write photo file to disk", Toast.LENGTH_SHORT).show()
            }
            // ---------------------------------------------------

        } else {
            Toast.makeText(context, "Capture failed or cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    val openCameraAction = {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                cameraLauncher.launch()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color(0xFF1A201B), RoundedCornerShape(16.dp))
                .border(2.dp, Color.DarkGray, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .clickable { openCameraAction() },
            contentAlignment = Alignment.Center
        ) {
            if (capturedBitmap != null) {
                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Captured",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { openCameraAction() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76B947))
        ) {
            Text(
                text = if (capturedBitmap == null) "OPEN CAMERA" else "RETAKE",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AudioRecorderView(onAudioSaved: (String) -> Unit) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }

    // Hold reference to the active hardware recorder instance and file
    var recorderInstance by remember { mutableStateOf<MediaRecorder?>(null) }
    var currentAudioFile by remember { mutableStateOf<File?>(null) }

    // Logic to turn on hardware microphone and start writing data stream
    val startRecording = {
        try {
            // Create a valid physical target file in your app sandbox
            val file = File(context.filesDir, "audio_${System.currentTimeMillis()}.m4a")
            currentAudioFile = file

            // Safely initialize depending on current Android OS API level
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION") MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) // Standard wrapper container
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)    // Clear digital audio compression format
                setOutputFile(file.absolutePath)
                prepare()
                start() // Hardware capture begins here
            }

            recorderInstance = recorder
            isRecording = true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not initialize hardware mic", Toast.LENGTH_SHORT).show()
        }
    }

    // Logic to safely cut off the stream and lock down the recorded file
    val stopRecording = { saveFile: Boolean ->
        recorderInstance?.let { recorder ->
            try {
                recorder.stop()
            } catch (e: Exception) {
                // Catches edge-case fast double clicks before recorder fills initial sample buffers
                e.printStackTrace()
            } finally {
                recorder.release()
            }
        }
        recorderInstance = null
        isRecording = false

        if (saveFile && currentAudioFile != null) {
            onAudioSaved(currentAudioFile!!.absolutePath)
            Toast.makeText(context, "Audio Saved!", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission handle check callback
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startRecording() else Toast.makeText(context, "Mic permission denied", Toast.LENGTH_SHORT).show()
    }

    // Automatic Max 5s countdown timer loop driver
    LaunchedEffect(isRecording) {
        if (isRecording) {
            seconds = 0
            while (seconds < 5) {
                delay(1000)
                seconds++
            }
            // Auto stop when max countdown limit hits
            stopRecording(true)
        }
    }

    // Clean up hardware resources immediately if user backs out of the screen mid-record
    DisposableEffect(Unit) {
        onDispose {
            recorderInstance?.let {
                try { it.stop() } catch (_: Exception) {}
                it.release()
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (isRecording) "Recording... $seconds / 5s" else "Max 5s Recording",
            color = if (isRecording) Color.Red else Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        FilledIconButton(
            onClick = {
                if (!isRecording) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        startRecording()
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                } else {
                    stopRecording(true) // User manually clicked stop early
                }
            },
            modifier = Modifier.size(80.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isRecording) Color.Red else Color(0xFF1A1A1B)
            )
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun EmojiSelectorView(onEmojiSelected: (String) -> Unit) {
    val emojis = listOf("🎯", "🔥", "👑", "🥶", "😱", "🥳", "🤫", "😭", "🤷‍♂️", "🍻", "💩", "🦉")
    LazyVerticalGrid(GridCells.Fixed(4), modifier = Modifier.fillMaxWidth(0.85f)) {
        items(emojis) { emoji ->
            Box(modifier = Modifier.size(60.dp).clickable { onEmojiSelected(emoji) }, contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 28.sp)
            }
        }
    }
}