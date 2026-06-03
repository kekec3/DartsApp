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
    var isPlaying by remember { mutableStateOf(false) }

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
                            model = File(context.filesDir, moment.contentValue),
                            contentDescription = "Moment Photo",
                            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    MomentType.AUDIO -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                if (isPlaying) {
                                    isPlaying = false
                                } else {
                                    val file = File(context.filesDir, moment.contentValue)
                                    if (file.exists()) {
                                        isPlaying = true
                                        MediaPlayer().apply {
                                            setAudioAttributes(
                                                AudioAttributes.Builder()
                                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                                    .build()
                                            )
                                            setDataSource(file.absolutePath)
                                            prepare()
                                            start()
                                            setOnCompletionListener {
                                                it.release()
                                                isPlaying = false
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "File not found: ${moment.contentValue}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Play",
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