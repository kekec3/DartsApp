package com.example.darts.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.darts.db.entities.Moment
import com.example.darts.db.entities.MomentType
import com.example.darts.db.repositories.MomentRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TimelineEntryPoint {
    fun momentRepository(): MomentRepository
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameTimelineScreen(
    gameId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val repository = remember {
        EntryPointAccessors.fromApplication(context, TimelineEntryPoint::class.java).momentRepository()
    }
    val moments by repository.getMomentsForGame(gameId).collectAsState(initial = emptyList())

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF0B0F0C))) {
        TopAppBar(
            title = { Text("GAME TIMELINE", fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111612))
        )

        // Using a basic Column if the list is small, or LazyColumn.
        // For the full winding path effect, drawing connections works best when items flow naturally.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(moments) { index, moment ->
                // Determine horizontal placement pattern: Left (0), Center (1), Right (2), Center (3)...
                val positionPattern = when (index % 4) {
                    0 -> Alignment.Start
                    1 -> Alignment.CenterHorizontally
                    2 -> Alignment.End
                    else -> Alignment.CenterHorizontally
                }

                val nextPositionPattern = if (index + 1 < moments.size) {
                    when ((index + 1) % 4) {
                        0 -> Alignment.Start
                        1 -> Alignment.CenterHorizontally
                        2 -> Alignment.End
                        else -> Alignment.CenterHorizontally
                    }
                } else null

                GameMapNodeRow(
                    moment = moment,
                    currentAlignment = positionPattern,
                    nextAlignment = nextPositionPattern
                )
            }
        }
    }
}

@Composable
fun GameMapNodeRow(
    moment: Moment,
    currentAlignment: Alignment.Horizontal,
    nextAlignment: Alignment.Horizontal?
) {
    val context = LocalContext.current
    var activePlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val isPlaying = activePlayer != null

    val physicalFile = remember(moment.contentValue) {
        val path = moment.contentValue
        if (path.startsWith("/")) File(path) else File(context.filesDir, path)
    }

    DisposableEffect(moment.idMoment) {
        onDispose {
            activePlayer?.let { player ->
                try { if (player.isPlaying) player.stop() } catch (_: Exception) {}
                player.release()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = when (currentAlignment) {
            Alignment.Start -> Alignment.CenterStart
            Alignment.End -> Alignment.CenterEnd
            else -> Alignment.Center
        }
    ) {
        // --- FIX: SMOOTH CURVED PATH LAYER ---
        if (nextAlignment != null) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    // Allow the canvas to draw slightly past its bounds into the next item row
                    .height(220.dp)
            ) {
                val startX = when (currentAlignment) {
                    Alignment.Start -> size.width * 0.16f
                    Alignment.End -> size.width * 0.84f
                    else -> size.width * 0.5f
                }
                val endX = when (nextAlignment) {
                    Alignment.Start -> size.width * 0.16f
                    Alignment.End -> size.width * 0.84f
                    else -> size.width * 0.5f
                }

                val startY = 27.dp.toPx() // Anchor directly behind the center of the 54.dp Node Bubble
                val endY = size.height   // Travel all the way down to meet the next node

                // Create an organic winding S-curve pathway
                val curvedPath = Path().apply {
                    moveTo(startX, startY)

                    // Control points pull the line outward to create the natural map bend
                    cubicTo(
                        x1 = startX, y1 = startY + (endY - startY) * 0.4f, // Control Point 1
                        x2 = endX, y2 = startY + (endY - startY) * 0.6f,   // Control Point 2
                        x3 = endX, y3 = endY                               // Target Destination
                    )
                }

                // Render the curved pathway track
                drawPath(
                    path = curvedPath,
                    color = Color(0xFF76B947).copy(alpha = 0.45f),
                    style = Stroke(
                        width = 7.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(20f, 18f), 0f
                        )
                    )
                )
            }
        }

        // --- INTERACTIVE CONTENT UI BLOCK ---
        Column(
            horizontalAlignment = currentAlignment,
            modifier = Modifier.fillMaxWidth(0.72f) // Prevents wide cards from overlaying other node bubbles
        ) {
            // Stylized Game Progress Hub Node
            Surface(
                modifier = Modifier
                    .size(54.dp)
                    .border(3.dp, Color(0xFF76B947), CircleShape),
                shape = CircleShape,
                color = Color(0xFF161B17),
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when(moment.type) {
                            MomentType.PHOTO -> Icons.Default.CameraAlt
                            MomentType.AUDIO -> Icons.Default.Mic
                            MomentType.EMOJI -> Icons.Default.EmojiEmotions
                        },
                        contentDescription = null,
                        tint = Color(0xFF76B947),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Info Details Popup Board
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2520)),
                border = BorderStroke(1.dp, Color(0xFF2C352E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    when(moment.type) {
                        MomentType.PHOTO -> {
                            AsyncImage(
                                model = physicalFile,
                                contentDescription = "Match Picture",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(135.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        MomentType.AUDIO -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (isPlaying) {
                                            activePlayer?.let { player ->
                                                try { if (player.isPlaying) player.stop() } catch (_: Exception) {}
                                                player.release()
                                            }
                                            activePlayer = null
                                        } else {
                                            if (physicalFile.exists()) {
                                                try {
                                                    val newPlayer = MediaPlayer().apply {
                                                        setAudioAttributes(
                                                            AudioAttributes.Builder()
                                                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                                                .build()
                                                        )
                                                        setDataSource(physicalFile.absolutePath)
                                                        prepare()
                                                        start()
                                                        setOnCompletionListener {
                                                            it.release()
                                                            activePlayer = null
                                                        }
                                                    }
                                                    activePlayer = newPlayer
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Playback error", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Audio missing!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF2C352E))
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF76B947)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (isPlaying) "Playing note..." else "Listen Voice Note",
                                    color = Color.LightGray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        MomentType.EMOJI -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(moment.contentValue, fontSize = 44.sp)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp)) // Added breathing space for the curve to sweep nicely
        }
    }
}

@Composable
fun TimelineItem(moment: Moment) {
    val context = LocalContext.current

    // Track the active native media player instance across recompositions
    var activePlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val isPlaying = activePlayer != null

    // Intelligently resolve whether the string is an absolute path or a legacy filename
    val physicalFile = remember(moment.contentValue) {
        val path = moment.contentValue
        if (path.startsWith("/")) File(path) else File(context.filesDir, path)
    }

    // Safely release hardware audio playback threads if user exits the screen mid-playback
    DisposableEffect(moment.idMoment) {
        onDispose {
            activePlayer?.let { player ->
                try { if (player.isPlaying) player.stop() } catch (_: Exception) {}
                player.release()
            }
        }
    }

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color(0xFF76B947)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when(moment.type) {
                            MomentType.PHOTO -> Icons.Default.CameraAlt
                            MomentType.AUDIO -> Icons.Default.Mic
                            MomentType.EMOJI -> Icons.Default.EmojiEmotions
                        },
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Box(modifier = Modifier.width(2.dp).weight(1f).background(Color(0xFF333333)))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                when(moment.type) {
                    MomentType.PHOTO -> {
                        AsyncImage(
                            model = physicalFile, // Fixed to use the resolved path safely
                            contentDescription = "Moment Photo",
                            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    MomentType.AUDIO -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                if (isPlaying) {
                                    // --- FIX: Stop and discard the active player instance ---
                                    activePlayer?.let { player ->
                                        try { if (player.isPlaying) player.stop() } catch (_: Exception) {}
                                        player.release()
                                    }
                                    activePlayer = null
                                } else {
                                    // --- FIX: Run safety validation checks on resolved path ---
                                    if (physicalFile.exists()) {
                                        try {
                                            val newPlayer = MediaPlayer().apply {
                                                setAudioAttributes(
                                                    AudioAttributes.Builder()
                                                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                        .setUsage(AudioAttributes.USAGE_MEDIA)
                                                        .build()
                                                )
                                                setDataSource(physicalFile.absolutePath)
                                                prepare()
                                                start()

                                                // Reset state cleanly when track finishes naturally
                                                setOnCompletionListener { completedPlayer ->
                                                    completedPlayer.release()
                                                    activePlayer = null
                                                }
                                            }
                                            activePlayer = newPlayer
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(context, "Playback error occurred", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Audio file missing from disk!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Stop" else "Play",
                                    tint = Color(0xFF76B947)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isPlaying) "Playing..." else "Play Voice Note", color = Color.White)
                        }
                    }
                    MomentType.EMOJI -> {
                        Text("Reaction", color = Color.Gray, fontSize = 12.sp)
                        Text(moment.contentValue, fontSize = 32.sp)
                    }
                }
            }
        }
    }
}