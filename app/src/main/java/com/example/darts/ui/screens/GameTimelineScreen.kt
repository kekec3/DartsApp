package com.example.darts.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
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

    Column(modifier = modifier.fillMaxSize().background(Color.Black)) {
        TopAppBar(
            title = { Text("Game Timeline", fontWeight = FontWeight.Bold, color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(moments) { moment ->
                TimelineItem(moment)
            }
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