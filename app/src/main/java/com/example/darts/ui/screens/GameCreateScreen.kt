package com.example.darts.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.darts.db.entities.Game
import com.example.darts.viewModel.GameCreationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameCreateScreen(
    battleId: Int,
    viewModel: GameCreationViewModel,
    onNewGame: () -> Unit,
    onLegSummary: (Int) -> Unit,    // Navigates to individual game stats
    onMatchSummary: (Int) -> Unit,  // Navigates to full battle stats
    onBack: () -> Unit
) {
    // 1. Initialize data loading for the specific battle context
    LaunchedEffect(battleId) {
        viewModel.loadBattle(battleId)
    }

    // 2. Observe the list of games belonging to this battle
    val games by viewModel.games.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Match Lobby",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onNewGame,
                containerColor = Color(0xFF76B947),
                shape = RoundedCornerShape(16.dp),
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Game",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- MATCH SUMMARY CARD ---
            // Leads to the total battle summary using the battleId
            MatchSummaryHeader(
                gameCount = games.size,
                onClick = { onMatchSummary(battleId) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "GAMES HISTORY",
                color = Color(0xFF76B947),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (games.isEmpty()) {
                EmptyGamesPlaceholder()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = games,
                        key = { it.idGame }
                    ) { game ->
                        // Each item leads to its own Leg Summary using the gameId
                        GameItem(
                            game = game,
                            onClick = { onLegSummary(game.idGame) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatchSummaryHeader(gameCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        border = BorderStroke(1.dp, Color(0xFF76B947).copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Match Overview",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "$gameCount games recorded in this session",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }

            Surface(
                color = Color(0xFF76B947).copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "VIEW STATS",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color(0xFF76B947),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun GameItem(game: Game, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFF252525), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = game.type.take(1).uppercase(),
                color = Color(0xFF76B947),
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Game #${game.idGame}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pin icon indicates a location-tagged leg
                if (game.location.isNotEmpty() && game.location != "0.0,0.0") {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF76B947),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "${game.type} • ${game.date}",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.DarkGray
        )
    }
}

@Composable
fun EmptyGamesPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No games yet",
                color = Color.DarkGray,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap + to start the first leg",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}