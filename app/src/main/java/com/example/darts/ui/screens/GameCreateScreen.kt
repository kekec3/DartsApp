package com.example.darts.ui.screens

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    onLegSummary: (Int) -> Unit,
    onMatchSummary: (Int) -> Unit,
    onBack: () -> Unit
) {
    // 1. Initialize data loading
    LaunchedEffect(battleId) {
        viewModel.loadBattle(battleId)
    }

    // 2. State Observation
    val games by viewModel.games.collectAsStateWithLifecycle()
    var isMapView by remember { mutableStateOf(false) }

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
                actions = {
                    // TOGGLE BUTTON: Switch between List and Map
                    IconButton(onClick = { isMapView = !isMapView }) {
                        Icon(
                            imageVector = if (isMapView) Icons.Default.List else Icons.Default.Place,
                            contentDescription = "Toggle View",
                            tint = if (isMapView) Color(0xFF76B947) else Color.White
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

        AnimatedContent(
            targetState = isMapView,
            label = "ViewTransition",
            modifier = Modifier.padding(innerPadding)
        ) { targetIsMapView ->
            if (targetIsMapView) {
                // --- CALLING YOUR STANDALONE MAP SCREEN ---
                MapScreen(games = games)
            } else {
                // --- LIST VIEW ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

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
                Text("Match Overview", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("$gameCount games recorded", color = Color.Gray, fontSize = 13.sp)
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
            Text(game.type.take(1).uppercase(), color = Color(0xFF76B947), fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Game #${game.idGame}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (game.location.isNotEmpty() && game.location != "0.0,0.0") {
                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFF76B947), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text("${game.type} • ${game.date}", color = Color.Gray, fontSize = 13.sp)
            }
        }
        Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.DarkGray)
    }
}

@Composable
fun EmptyGamesPlaceholder() {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No games yet", color = Color.DarkGray, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Tap + to start the first leg", color = Color.Gray, fontSize = 14.sp)
        }
    }
}