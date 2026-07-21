package com.example.darts.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.darts.viewModel.GameMode
import com.example.darts.viewModel.MatchPlayerStats
import com.example.darts.viewModel.MatchSummaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    val winner = state.players.firstOrNull()

    BackHandler {
        onBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            title = { Text("Match Summary", fontWeight = FontWeight.Bold, color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Match Lobby",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Trophy / Star Icon
            Surface(
                modifier = Modifier.size(90.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${winner?.playerName ?: "Player"} Wins!",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                text = if (isCricket) "Cricket Match Summary" else "X01 Match Summary",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Players Leaderboard List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(state.players) { index, playerStats ->
                    MatchPlayerCard(
                        rank = index + 1,
                        playerStats = playerStats,
                        isCricket = isCricket
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 8.dp),
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
fun MatchPlayerCard(
    rank: Int,
    playerStats: MatchPlayerStats,
    isCricket: Boolean
) {
    val rankColor = when (rank) {
        1 -> Color(0xFF76B947)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color.White
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rank == 1) Color(0xFF1B2E1B) else Color(0xFF1E1E1E)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#$rank",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = rankColor,
                        modifier = Modifier.width(32.dp)
                    )
                    Text(
                        text = playerStats.playerName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF2A302A)
                ) {
                    Text(
                        text = "${playerStats.legsWon} ${if (playerStats.legsWon == 1) "Leg" else "Legs"}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = Color(0xFF2A2F2A)
            )

            if (isCricket) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MatchStatTile(label = "MPR", value = "%.2f".format(playerStats.average))
                    MatchStatTile(label = "Legs Won", value = "${playerStats.legsWon}")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MatchStatTile(label = "3-Dart Avg", value = "%.2f".format(playerStats.average))
                    MatchStatTile(label = "Checkout %", value = "%.1f%%".format(playerStats.checkoutPercent))
                    MatchStatTile(label = "High Checkout", value = "${playerStats.highestCheckout}")
                    MatchStatTile(label = "180s", value = "${playerStats.scores180}")
                }
            }
        }
    }
}

@Composable
private fun MatchStatTile(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}