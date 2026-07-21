package com.example.darts.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.darts.viewModel.LegPlayerStat
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
    val isCricket = state.gameMode == GameMode.CRICKET
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = "Exit Leg Summary",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Do you want to exit the summary screen?",
                    color = Color.Gray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onBack()
                    }
                ) {
                    Text("Yes", color = Color(0xFF76B947), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("No", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            title = { Text("Leg $legNumber Summary", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header card with centered win text
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = CircleShape,
                        color = Color(0xFF1B5E20)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Leg Winner",
                                modifier = Modifier.size(36.dp),
                                tint = Color(0xFF76B947)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (state.winnerName.isNotEmpty()) "${state.winnerName} wins Leg $legNumber!" else "Leg Completed",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = if (isCricket) "Cricket" else "X01",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Player Stats List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.players) { playerStat ->
                    LegPlayerCard(playerStat = playerStat, isCricket = isCricket)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76B947))
            ) {
                Text("CONTINUE", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun LegPlayerCard(
    playerStat: LegPlayerStat,
    isCricket: Boolean
) {
    val stats = playerStat.stats
    val isWinner = playerStat.won

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWinner) Color(0xFF1B2E1B) else Color(0xFF1E1E1E)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = playerStat.playerName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isWinner) Color(0xFF76B947) else Color.White
                )
                if (isWinner) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF76B947)
                    ) {
                        Text(
                            text = "WINNER",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = Color(0xFF2A2F2A)
            )

            if (isCricket) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatTile(label = "MPR", value = "%.2f".format(stats.average))
                    StatTile(label = "Darts Thrown", value = "${stats.dartsThrown}")
                    StatTile(label = "Points", value = "${stats.totalScored}")
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatTile(label = "3-Dart Avg", value = "%.2f".format(stats.average))
                        StatTile(label = "Darts Thrown", value = "${stats.dartsThrown}")
                        StatTile(label = "Checkout %", value = checkoutPercent(stats))
                        StatTile(label = "High Checkout", value = "${stats.highestCheckout}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatTile(label = "100+", value = "${stats.scores100Plus}")
                        StatTile(label = "140+", value = "${stats.scores140Plus}")
                        StatTile(label = "180s", value = "${stats.scores180}")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

private fun checkoutPercent(stats: PlayerLegStats): String {
    if (stats.checkoutAttempts == 0) return "0%"
    return "%.1f%%".format(stats.checkoutsHit * 100f / stats.checkoutAttempts)
}