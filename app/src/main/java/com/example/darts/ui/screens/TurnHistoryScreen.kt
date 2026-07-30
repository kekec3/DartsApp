package com.example.darts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darts.ui.theme.LimePrimary
import com.example.darts.ui.theme.TextSecondary
import com.example.darts.viewModel.TurnSummary

@Composable
fun TurnHistoryScreen(
    turns: List<TurnSummary>,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .systemBarsPadding()
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close history",
                    tint               = Color.White
                )
            }
            Text(
                text          = "TURN HISTORY",
                color         = Color.White,
                fontSize      = 20.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                textAlign     = TextAlign.Center,
                modifier      = Modifier.weight(1f)
            )
            Text(
                text     = "${turns.size} turns",
                color    = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(end = 16.dp)
            )
        }

        HorizontalDivider(color = DividerColor, thickness = 1.dp)

        // ── List or empty state ───────────────────────────────────────────────
        if (turns.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "No turns yet",
                    color = TextSecondary,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier        = Modifier.fillMaxSize(),
                contentPadding  = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout   = true   // most recent turn at the top
            ) {
                itemsIndexed(turns) { _, turn ->
                    TurnHistoryCard(turn = turn)
                }
            }
        }
    }
}

// ── Turn card ─────────────────────────────────────────────────────────────────

@Composable
private fun TurnHistoryCard(turn: TurnSummary) {
    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = CardInactive,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Turn number + player name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text          = "T${turn.turnNumber}",
                    color         = TextSecondary,
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text          = turn.playerName.uppercase(),
                    color         = Color.White,
                    fontSize      = 14.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            // Dart chips — always shows 3 slots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.weight(2f)
            ) {
                turn.dartDisplays.forEach { dart -> DartChip(label = dart) }
                repeat(3 - turn.dartDisplays.size) { DartChip(label = "—", empty = true) }
            }

            // Turn score + remaining
            Column(
                horizontalAlignment = Alignment.End,
                modifier            = Modifier.weight(1f)
            ) {
                Text(
                    text       = "+${turn.turnScore}",
                    color      = LimePrimary,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                turn.remainingAfter?.let { rem ->
                    Text(
                        text     = "$rem left",
                        color    = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DartChip(label: String, empty: Boolean = false) {
    Box(
        modifier = Modifier
            .background(
                color  = if (empty) Color(0xFF0F1510) else Color(0xFF252B26),
                shape  = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            color      = if (empty) Color(0xFF2A3030) else Color.White,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}