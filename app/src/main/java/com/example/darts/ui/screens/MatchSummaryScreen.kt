package com.example.darts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.darts.viewModel.GameMode
import com.example.darts.viewModel.MatchSummaryViewModel

@Composable
fun MatchSummaryScreen(
    modifier: Modifier = Modifier,
    gameId: Int,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit = {},
    viewModel: MatchSummaryViewModel = hiltViewModel()
) {
    LaunchedEffect(gameId) {
        viewModel.load(gameId)
    }

    val state by viewModel.uiState.collectAsState()
    val isCricket = state.gameMode == GameMode.CRICKET

    val winner = state.players.maxByOrNull { it.legsWon }
    val loser  = state.players.firstOrNull { it.playerId != winner?.playerId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = Color(0xFF1B5E20)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Winner",
                        modifier = Modifier.size(50.dp),
                        tint = Color(0xFF76B947)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "${winner?.playerName ?: ""} Wins!",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "${winner?.legsWon ?: 0} - ${loser?.legsWon ?: 0}",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (isCricket) {
                // MPR is stored in the average field
                MatchStatRow(
                    "MPR",
                    "%.2f".format(winner?.average ?: 0f),
                    "%.2f".format(loser?.average ?: 0f),
                    (winner?.average ?: 0f) > (loser?.average ?: 0f)
                )
                MatchStatRow(
                    "Legs Won",
                    "${winner?.legsWon ?: 0}",
                    "${loser?.legsWon ?: 0}",
                    true
                )
            } else {
                MatchStatRow(
                    "3-Dart Average",
                    "%.2f".format(winner?.average ?: 0f),
                    "%.2f".format(loser?.average ?: 0f),
                    (winner?.average ?: 0f) > (loser?.average ?: 0f)
                )
                MatchStatRow(
                    "Checkout %",
                    "%.1f%%".format(winner?.checkoutPercent ?: 0f),
                    "%.1f%%".format(loser?.checkoutPercent ?: 0f),
                    (winner?.checkoutPercent ?: 0f) > (loser?.checkoutPercent ?: 0f)
                )
                MatchStatRow(
                    "Highest Checkout",
                    "${winner?.highestCheckout ?: 0}",
                    "${loser?.highestCheckout ?: 0}",
                    (winner?.highestCheckout ?: 0) > (loser?.highestCheckout ?: 0)
                )
                MatchStatRow(
                    "180s",
                    "${winner?.scores180 ?: 0}",
                    "${loser?.scores180 ?: 0}",
                    (winner?.scores180 ?: 0) > (loser?.scores180 ?: 0)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onNavigateHome,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76B947))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "BACK TO HOME",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MatchStatRow(
    label: String,
    leftVal: String,
    rightVal: String,
    highlightLeft: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = leftVal,
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlightLeft) Color(0xFF76B947) else Color.White,
            textAlign = TextAlign.Start
        )
        Text(
            text = label,
            modifier = Modifier.weight(2f),
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Text(
            text = rightVal,
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.End
        )
    }
}