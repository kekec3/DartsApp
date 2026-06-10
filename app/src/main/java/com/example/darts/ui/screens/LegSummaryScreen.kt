package com.example.darts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.darts.db.entities.PlayerLegStats
import com.example.darts.viewModel.GameMode
import com.example.darts.viewModel.LegSummaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegSummaryScreen(
    modifier: Modifier = Modifier,
    gameId: Int,
    legNumber: Int,
    onBack: () -> Unit,
    onContinue: () -> Unit = {},
    viewModel: LegSummaryViewModel = hiltViewModel()
) {
    LaunchedEffect(gameId, legNumber) {
        viewModel.load(gameId, legNumber)
    }

    val state by viewModel.uiState.collectAsState()
    val p1 = state.player1Stats
    val p2 = state.player2Stats
    val winnerLeft = p1?.won == true
    val isCricket = state.gameMode == GameMode.CRICKET

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            title = { Text("Leg Summary", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            state.player1Name,
                            fontSize = 20.sp,
                            color = if (winnerLeft) Color(0xFF76B947) else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text("VS", fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(
                            state.player2Name,
                            fontSize = 20.sp,
                            color = if (!winnerLeft) Color(0xFF76B947) else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (isCricket) "Cricket" else "501",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isCricket) {
                    // Cricket stats: MPR is stored in the average field
                    StatComparisonRow(
                        "MPR",
                        "%.2f".format(p1?.average ?: 0f),
                        "%.2f".format(p2?.average ?: 0f),
                        (p1?.average ?: 0f) > (p2?.average ?: 0f)
                    )
                    StatComparisonRow(
                        "Darts Thrown",
                        "${p1?.dartsThrown ?: 0}",
                        "${p2?.dartsThrown ?: 0}",
                        (p1?.dartsThrown ?: 0) < (p2?.dartsThrown ?: 0) // fewer is better
                    )
                } else {
                    // X01 stats
                    StatComparisonRow(
                        "3-Dart Average",
                        "%.2f".format(p1?.average ?: 0f),
                        "%.2f".format(p2?.average ?: 0f),
                        (p1?.average ?: 0f) > (p2?.average ?: 0f)
                    )
                    StatComparisonRow(
                        "Highest Checkout",
                        "${p1?.highestCheckout ?: 0}",
                        "${p2?.highestCheckout ?: 0}",
                        (p1?.highestCheckout ?: 0) > (p2?.highestCheckout ?: 0)
                    )
                    StatComparisonRow(
                        "Checkout %",
                        checkoutPercent(p1),
                        checkoutPercent(p2),
                        percentValue(p1) > percentValue(p2)
                    )
                    StatComparisonRow(
                        "100+ Scores",
                        "${p1?.scores100Plus ?: 0}",
                        "${p2?.scores100Plus ?: 0}"
                    )
                    StatComparisonRow(
                        "140+ Scores",
                        "${p1?.scores140Plus ?: 0}",
                        "${p2?.scores140Plus ?: 0}"
                    )
                    StatComparisonRow(
                        "180s",
                        "${p1?.scores180 ?: 0}",
                        "${p2?.scores180 ?: 0}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76B947))
            ) {
                Text("CONTINUE", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
    }
}

private fun checkoutPercent(stats: PlayerLegStats?): String {
    if (stats == null || stats.checkoutAttempts == 0) return "0%"
    return "%.1f%%".format(stats.checkoutsHit * 100f / stats.checkoutAttempts)
}

private fun percentValue(stats: PlayerLegStats?): Float {
    if (stats == null || stats.checkoutAttempts == 0) return 0f
    return stats.checkoutsHit * 100f / stats.checkoutAttempts
}

@Composable
fun StatComparisonRow(
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
            fontSize = 18.sp,
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
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.End
        )
    }
}