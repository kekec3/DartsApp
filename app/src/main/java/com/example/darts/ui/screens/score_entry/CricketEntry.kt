package com.example.darts.ui.screens.score_entry

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darts.engine.DartThrow
import com.example.darts.engine.Multiplier
import com.example.darts.ui.theme.LimePrimary
import com.example.darts.ui.theme.TextSecondary
import com.example.darts.viewModel.CricketUiState
import com.example.darts.viewModel.states.CricketNumber
import com.example.darts.viewModel.states.PlayerStateCricket

private val BtnSurface = Color(0xFF252B26)
private val BtnText    = Color(0xFFEEF2EE)
// Strikethrough color: neutral gray instead of red
private val StrikeColor = Color(0xFF888888)

@Composable
fun CricketEntry(
    cricketState: CricketUiState,
    dartsEntered: Int,
    onDartAdded: (DartThrow) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var multiplier by remember { mutableStateOf(Multiplier.SINGLE) }

    // Track this turn's darts locally for real-time mark preview
    val localDarts = remember { mutableStateListOf<DartThrow>() }

    // Sync localDarts whenever dartsEntered changes — handles both undo and turn reset
    LaunchedEffect(dartsEntered) {
        while (localDarts.size > dartsEntered) localDarts.removeLast()
    }

    // Compute preview player states by applying localDarts on top of the confirmed state.
    // Reading localDarts (SnapshotStateList) here automatically triggers recomposition on change.
    val curr = cricketState.currentPlayerIndex
    val previewPlayerStates: List<PlayerStateCricket> = cricketState.playerStates
        .toMutableList()
        .also { states ->
            for (dart in localDarts) {
                val number = dart.value
                if (number == 0 || number !in (states[curr].numbers)) continue
                val currentMarks = states[curr].numbers[number]?.marks ?: 0
                if (currentMarks >= 3) continue
                val newMarks = (currentMarks + dart.multiplier.mul).coerceAtMost(3)
                states[curr] = states[curr].copy(
                    numbers = states[curr].numbers.toMutableMap().apply {
                        put(number, CricketNumber(newMarks))
                    }
                )
            }
        }

    fun addAndReset(dart: DartThrow) {
        localDarts.add(dart)
        onDartAdded(dart)
        multiplier = Multiplier.SINGLE
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Top
    ) {
        DartProgressDots(dartsEntered = dartsEntered)

        Spacer(Modifier.height(10.dp))

        MultiplierSelector(
            selected = multiplier,
            onSelect = { multiplier = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
        )

        Spacer(Modifier.height(10.dp))

        val cricketNumbers = listOf(20, 19, 18, 17, 16, 15, 25)
        cricketNumbers.forEach { num ->
            CricketRow(
                number = num,
                multiplier = multiplier,
                players = previewPlayerStates,          // ← live preview instead of confirmed state
                currentPlayerIndex = cricketState.currentPlayerIndex,
                onNumberClick = { n, m -> addAndReset(DartThrow(n, m)) }
            )
        }

        Spacer(Modifier.height(10.dp))

        // Bottom row: MISS + UNDO, weight-based so they fill the width like cricket rows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // MISS
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LimePrimary)
                    .clickable { addAndReset(DartThrow(0, Multiplier.SINGLE)) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "0",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "MISS",
                        color = Color.Black.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 9.sp
                    )
                }
            }

            // UNDO
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BtnSurface)
                    .clickable { onUndo() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = BtnText,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun CricketRow(
    number: Int,
    multiplier: Multiplier,
    players: List<PlayerStateCricket>,
    currentPlayerIndex: Int,
    onNumberClick: (Int, Multiplier) -> Unit
) {
    val isClosedByAll = players.isNotEmpty() && players.all { (it.numbers[number]?.marks ?: 0) >= 3 }

    val midPoint = (players.size + 1) / 2
    val leftPlayers  = players.take(midPoint)
    val rightPlayers = players.drop(midPoint)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
            leftPlayers.forEachIndexed { index, player ->
                val isCurrent = index == currentPlayerIndex
                CricketMarkCanvas(
                    marks = player.numbers[number]?.marks ?: 0,
                    color = if (isCurrent) LimePrimary else TextSecondary
                )
            }
        }

        val bgColor  = if (isClosedByAll) Color(0xFF1A201B) else BtnSurface
        val txtColor = if (isClosedByAll) TextSecondary.copy(alpha = 0.4f) else BtnText

        Box(
            modifier = Modifier
                .weight(1.5f)
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor)
                .clickable(enabled = !isClosedByAll) { onNumberClick(number, multiplier) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (number == 25) "BULL" else number.toString(),
                color = txtColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (isClosedByAll) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(
                        color = StrikeColor.copy(alpha = 0.7f),   // gray, not red
                        start = Offset(10f, size.height / 2),
                        end   = Offset(size.width - 10f, size.height / 2),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
        }

        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
            rightPlayers.forEachIndexed { index, player ->
                val actualIndex = index + midPoint
                val isCurrent   = actualIndex == currentPlayerIndex
                CricketMarkCanvas(
                    marks = player.numbers[number]?.marks ?: 0,
                    color = if (isCurrent) LimePrimary else TextSecondary
                )
            }
        }
    }
}

@Composable
fun CricketMarkCanvas(marks: Int, color: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val stroke = 2.5.dp.toPx()

        if (marks >= 1) {
            drawLine(
                color = color,
                start = Offset(0f, 0f),
                end   = Offset(size.width, size.height),
                strokeWidth = stroke
            )
        }
        if (marks >= 2) {
            drawLine(
                color = color,
                start = Offset(size.width, 0f),
                end   = Offset(0f, size.height),
                strokeWidth = stroke
            )
        }
        if (marks >= 3) {
            drawCircle(
                color  = color,
                radius = size.width / 2.2f,
                center = Offset(size.width / 2f, size.height / 2f),
                style  = Stroke(width = stroke)
            )
        }
    }
}