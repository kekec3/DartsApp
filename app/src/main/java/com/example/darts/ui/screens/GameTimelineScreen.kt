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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.darts.db.entities.Moment
import com.example.darts.db.entities.MomentType
import com.example.darts.db.repositories.GameRepository
import com.example.darts.db.repositories.MomentRepository
import com.example.darts.viewModel.TurnSummary
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File

// --- UNIFIED TIMELINE MODEL ---
sealed interface TimelineEvent {
    val turnNumber: Int

    data class Turn(
        val summary: TurnSummary
    ) : TimelineEvent {
        override val turnNumber: Int = summary.turnNumber
    }

    data class MatchMoment(
        val moment: Moment
    ) : TimelineEvent {
        override val turnNumber: Int = moment.turnNumber
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TimelineEntryPoint {
    fun momentRepository(): MomentRepository
    fun gameRepository(): GameRepository
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameTimelineScreen(
    gameId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context, TimelineEntryPoint::class.java)
    }

    val momentRepository = entryPoint.momentRepository()
    val gameRepository = entryPoint.gameRepository()

    // ── Data Streams ────────────────────────────────────────────────────────
    val moments by momentRepository.getMomentsForGame(gameId).collectAsState(initial = emptyList())
    var turnHistory by remember { mutableStateOf<List<TurnSummary>>(emptyList()) }

    // Read and deserialize post-match JSON data snapshot
    LaunchedEffect(gameId) {
        val game = gameRepository.getGameById(gameId)
        if (game?.history != null) {
            try {
                val type = object : TypeToken<List<TurnSummary>>() {}.type
                turnHistory = Gson().fromJson(game.history, type)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ── Chronological Interleaving Engine ──────────────────────────────────
    val combinedTimeline = remember(moments, turnHistory) {
        val events = mutableListOf<TimelineEvent>()

        // Group captured media contextual blocks by their designated turn number anchors
        val momentsByTurn = moments.groupBy { it.turnNumber }

        // Safely extract and inject untagged/pre-game moments before turn records begin
        momentsByTurn[0]?.forEach { events.add(TimelineEvent.MatchMoment(it)) }

        // Sort match progression turns sequentially and interleave matched moments immediately after
        turnHistory.sortedBy { it.turnNumber }.forEach { turn ->
            events.add(TimelineEvent.Turn(turn))
            momentsByTurn[turn.turnNumber]?.forEach { moment ->
                events.add(TimelineEvent.MatchMoment(moment))
            }
        }
        events
    }

    // ── Main UI Layout ──────────────────────────────────────────────────────
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

        if (combinedTimeline.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No timeline events recorded", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(combinedTimeline) { index, event ->
                    // Dynamic horizontal zig-zag coordinate mapper
                    val positionPattern = when (index % 4) {
                        0 -> Alignment.Start
                        1 -> Alignment.CenterHorizontally
                        2 -> Alignment.End
                        else -> Alignment.CenterHorizontally
                    }

                    val nextPositionPattern = if (index + 1 < combinedTimeline.size) {
                        when ((index + 1) % 4) {
                            0 -> Alignment.Start
                            1 -> Alignment.CenterHorizontally
                            2 -> Alignment.End
                            else -> Alignment.CenterHorizontally
                        }
                    } else null

                    GameMapNodeRow(
                        event = event,
                        currentAlignment = positionPattern,
                        nextAlignment = nextPositionPattern
                    )
                }
            }
        }
    }
}

@Composable
fun GameMapNodeRow(
    event: TimelineEvent,
    currentAlignment: Alignment.Horizontal,
    nextAlignment: Alignment.Horizontal?
) {
    val context = LocalContext.current

    // Configurable height based on card type to prevent curved track breaks
    val containerHeight = remember(event) {
        if (event is TimelineEvent.Turn) 170.dp else 230.dp
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = when (currentAlignment) {
            Alignment.Start -> Alignment.CenterStart
            Alignment.End -> Alignment.CenterEnd
            else -> Alignment.Center
        }
    ) {
        // ── Curved Connection Path Layer ─────────────────────────────────────
        if (nextAlignment != null) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(containerHeight)
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

                val startY = 27.dp.toPx()
                val endY = size.height

                val curvedPath = Path().apply {
                    moveTo(startX, startY)
                    cubicTo(
                        x1 = startX, y1 = startY + (endY - startY) * 0.4f,
                        x2 = endX, y2 = startY + (endY - startY) * 0.6f,
                        x3 = endX, y3 = endY
                    )
                }

                drawPath(
                    path = curvedPath,
                    color = Color(0xFF76B947).copy(alpha = 0.45f),
                    style = Stroke(
                        width = 7.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 18f), 0f)
                    )
                )
            }
        }

        // ── Timeline Hub Node & Content Core ────────────────────────────────
        Column(
            horizontalAlignment = currentAlignment,
            modifier = Modifier.fillMaxWidth(0.78f)
        ) {
            // Stylized Game Progress Hub Node Indicator Bubble
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
                        imageVector = when (event) {
                            is TimelineEvent.Turn -> Icons.Default.Adjust
                            is TimelineEvent.MatchMoment -> when (event.moment.type) {
                                MomentType.PHOTO -> Icons.Default.CameraAlt
                                MomentType.AUDIO -> Icons.Default.Mic
                                MomentType.EMOJI -> Icons.Default.EmojiEmotions
                            }
                        },
                        contentDescription = null,
                        tint = Color(0xFF76B947),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Info Details Popup Board Card Selector Switching
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2520)),
                border = BorderStroke(1.dp, Color(0xFF2C352E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    when (event) {
                        is TimelineEvent.Turn -> TimelineTurnCardContent(turn = event.summary)
                        is TimelineEvent.MatchMoment -> TimelineMomentCardContent(moment = event.moment, context = context)
                    }
                }
            }
            Spacer(modifier = Modifier.height(35.dp))
        }
    }
}

// ── Shared Content UI Subcomponents ──────────────────────────────────────────

@Composable
private fun TimelineTurnCardContent(turn: TurnSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.1f)) {
            Text(
                text = "TURN ${turn.turnNumber}",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
            Text(
                text = turn.playerName.uppercase(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(2f)
        ) {
            turn.dartDisplays.forEach { dart -> MiniDartChip(label = dart) }
            repeat(3 - turn.dartDisplays.size) { MiniDartChip(label = "—", empty = true) }
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "+${turn.turnScore}",
                color = Color(0xFF76B947),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            turn.remainingAfter?.let { rem ->
                Text(
                    text = "$rem left",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun TimelineMomentCardContent(moment: Moment, context: android.content.Context) {
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

    when (moment.type) {
        MomentType.PHOTO -> {
            AsyncImage(
                model = physicalFile,
                contentDescription = "Match Snapshot",
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
                                Toast.makeText(context, "Audio file missing!", Toast.LENGTH_SHORT).show()
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
                Text(moment.contentValue, fontSize = 44.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun MiniDartChip(label: String, empty: Boolean = false) {
    Box(
        modifier = Modifier
            .background(
                color = if (empty) Color(0xFF0F1510) else Color(0xFF252B26),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (empty) Color(0xFF2A3030) else Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}