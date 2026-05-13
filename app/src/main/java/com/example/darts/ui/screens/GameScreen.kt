package com.example.darts.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darts.ui.screens.score_entry.ArrowIndicator
import com.example.darts.ui.screens.score_entry.BoardButtonsEntry
import com.example.darts.ui.screens.score_entry.CameraScanScreen
import com.example.darts.ui.screens.score_entry.EntryMethod
import com.example.darts.ui.screens.score_entry.EntryMethodBar
import com.example.darts.ui.screens.score_entry.PlayerCardMinimal
import com.example.darts.ui.screens.score_entry.ScoreInputEntry
import com.example.darts.ui.screens.score_entry.VoiceRecognitionScreen
import com.example.darts.ui.theme.LimePrimary
import com.example.darts.ui.theme.TextSecondary
import com.example.darts.viewModel.BaseGameViewModel
import com.example.darts.viewModel.states.DartSlotState
import com.example.darts.viewModel.states.PlayerDisplayState
import com.example.darts.viewModel.states.TurnDisplayState

// ── Shared colour palette ──────────────────────────────────────────────────────
val ScreenBg       = Color(0xFF0B0F0C)   // BlackPrimary from theme
val CardActive     = LimePrimary   // active-player card bg
val CardInactive   = Color(0xFF1A201B)   // BlackSurface from theme
val TurnOrange     = LimePrimary
val HeaderWhite    = Color.White
val MethodBarBg    = Color(0xFF121712)   // BlackSecondary from theme
val DividerColor   = Color(0xFF2A2F2A)   // Divider from theme

@Composable
fun GameScreen(
    viewModel: BaseGameViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateStats: () -> Unit = {}
) {
    val state  by viewModel.displayState.collectAsState()
    val method by viewModel.activeEntryMethod.collectAsState()

    if (state.isFinished && state.winner != null) {
        WinnerScreen(winnerName = state.winner!!, onBack = onNavigateBack)
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            GameTopBar(
                title   = state.gameTitle,
                onBack  = onNavigateBack,
                onStats = onNavigateStats
            )

            // ── Player cards ──────────────────────────────────────────────────
            PlayerRowBig(
                players  = state.players,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            // ── Turn banner ───────────────────────────────────────────────────
            Row(
                modifier          = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArrowIndicator(color = LimePrimary, size = 10.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text          = "${state.turn.currentPlayerName.uppercase()}'S TURN",
                    color         = TurnOrange,
                    fontSize      = 15.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
            }

            // ── Dart slots bar ────────────────────────────────────────────────
            DartSlotsBarNew(
                turn     = state.turn,
                method   = method,
                onSubmit = viewModel::commitTurn,
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            // ── Entry panel ───────────────────────────────────────────────────
            Box(
                modifier         = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                AnimatedContent(
                    targetState  = method,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label        = "entry_content"
                ) { activeMethod ->
                    Surface(
                        tonalElevation = 2.dp,
                        color          = Color(0xFF121712),
                        shape          = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        modifier       = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)) {
                            when (activeMethod) {
                                is EntryMethod.BoardButtons -> BoardButtonsEntry(
                                    dartsEntered = state.turn.dartsEnteredCount,
                                    onDartAdded  = viewModel::addDart,
                                    onUndo       = viewModel::undoLastDart
                                )
                                is EntryMethod.ScoreInput -> ScoreInputEntry(
                                    onScoreEntered = { dart ->
                                        viewModel.addDart(dart)
                                        viewModel.commitTurn()
                                    }
                                )
                                is EntryMethod.Voice  -> VoiceRecognitionScreen()
                                is EntryMethod.Camera -> CameraScanScreen()
                            }
                        }
                    }
                }
            }

            // ── Method selector bar ───────────────────────────────────────────
            EntryMethodBar(
                methods        = viewModel.supportedEntryMethods,
                selectedMethod = method,
                onSelect       = viewModel::setEntryMethod,
                modifier       = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun GameTopBar(title: String, onBack: () -> Unit, onStats: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = HeaderWhite)
        }
        Text(
            text       = title,
            color      = HeaderWhite,
            fontSize   = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier   = Modifier.weight(1f),
            textAlign  = TextAlign.Center
        )
        IconButton(onClick = onStats) {
            Icon(Icons.Default.TrendingUp, "Stats", tint = HeaderWhite)
        }
        IconButton(onClick = {}) {
            Icon(Icons.Default.Settings, "Settings", tint = HeaderWhite)
        }
    }
}

// ── Player row ────────────────────────────────────────────────────────────────

@Composable
private fun PlayerRowBig(
    players: List<PlayerDisplayState>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        players.forEach { player ->
            PlayerCardMinimal(
                player   = player,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Dart slots bar ────────────────────────────────────────────────────────────

@Composable
private fun DartSlotsBarNew(
    turn: TurnDisplayState,
    method: EntryMethod,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBust = turn.isBust

    Surface(
        modifier        = modifier.height(64.dp),
        shape           = RoundedCornerShape(32.dp),
        color           = if (isBust) Color(0xFF2A1010) else Color(0xFF1A201B),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier          = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Method icon
            Box(
                modifier         = Modifier
                    .size(60.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = methodIcon(method),
                    contentDescription = null,
                    tint               = if (isBust) TurnOrange else TextSecondary,
                    modifier           = Modifier.size(24.dp)
                )
            }

            // Dart slots
            turn.slots.forEach { slot ->
                VerticalDivider(
                    color     = DividerColor,
                    thickness = 1.dp,
                    modifier  = Modifier.height(34.dp)
                )
                DartSlotCell(
                    slot     = slot,
                    isBust   = isBust,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // Submit / Bust
            Box(modifier = Modifier.padding(end = 7.dp, top = 5.dp, bottom = 5.dp)) {
                if (isBust) {
                    Box(
                        modifier = Modifier
                            .height(54.dp)
                            .width(104.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(TurnOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "BUST",
                            color         = Color.White,
                            fontWeight    = FontWeight.ExtraBold,
                            fontSize      = 16.sp,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    Button(
                        onClick  = onSubmit,
                        enabled  = turn.dartsEnteredCount > 0,
                        shape    = RoundedCornerShape(25.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .width(104.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = LimePrimary,
                            contentColor           = Color(0xFF0B0F0C),
                            disabledContainerColor = Color(0xFF2A3030),
                            disabledContentColor   = Color(0xFF4A5555)
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "SUBMIT",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 13.sp,
                                color      = Color(0xFF0B0F0C)
                            )
                            if (turn.dartsEnteredCount > 0) {
                                Text(
                                    "+${turn.turnScore}",
                                    fontSize   = 11.sp,
                                    color      = Color(0xFF0B0F0C).copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DartSlotCell(
    slot: DartSlotState,
    isBust: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier         = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            slot.dart != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text       = slot.dart.displayString(),
                    color      = if (isBust) TurnOrange else Color.White,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text     = "${slot.dart.score()}",
                    color    = if (isBust) TurnOrange.copy(alpha = 0.6f) else TextSecondary,
                    fontSize = 11.sp
                )
            }
            else -> Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(DividerColor)
            )
        }
    }
}

// ── Winner screen ─────────────────────────────────────────────────────────────

@Composable
private fun WinnerScreen(winnerName: String, onBack: () -> Unit) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(ScreenBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier            = Modifier.padding(horizontal = 40.dp)
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint     = Color(0xFFFFD700),
                modifier = Modifier.size(96.dp)
            )
            Text(
                "GAME SHOT!",
                color         = Color.White,
                fontSize      = 32.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Text(
                winnerName.uppercase(),
                color      = LimePrimary,
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = onBack,
                colors   = ButtonDefaults.buttonColors(containerColor = LimePrimary),
                shape    = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Back to Menu",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = Color(0xFF0B0F0C)
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun methodIcon(method: EntryMethod): ImageVector = when (method) {
    is EntryMethod.BoardButtons -> Icons.Default.GridOn
    is EntryMethod.ScoreInput   -> Icons.Default.Keyboard
    is EntryMethod.Voice        -> Icons.Default.Mic
    is EntryMethod.Camera       -> Icons.Default.Videocam
}