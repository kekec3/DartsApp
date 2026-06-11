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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
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
    onLegSummary: (Int, Int) -> Unit,
    onMatchSummary: (Int) -> Unit,
    onMomentsTimeline: (Int) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(battleId) {
        viewModel.loadBattle(battleId)
    }

    val games by viewModel.games.collectAsStateWithLifecycle()
    var isMapView by remember { mutableStateOf(false) }

    // Removed Scaffold, using Box for floating button overlay
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        Column(modifier = Modifier.fillMaxSize()) {
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

            AnimatedContent(
                targetState = isMapView,
                label = "ViewTransition",
                modifier = Modifier.fillMaxSize()
            ) { targetIsMapView ->
                if (targetIsMapView) {
                    MapScreen(games = games)
                } else {
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
                                contentPadding = PaddingValues(bottom = 100.dp) // Added padding for FAB
                            ) {
                                items(
                                    items = games,
                                    key = { it.idGame }
                                ) { game ->
                                    GameItem(
                                        game = game,
                                        onClick = { onMatchSummary(game.idGame) },
                                        onMomentsTimelineClick = { onMomentsTimeline(game.idGame) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        LargeFloatingActionButton(
            onClick = onNewGame,
            containerColor = Color(0xFF76B947),
            shape = RoundedCornerShape(16.dp),
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Game",
                modifier = Modifier.size(32.dp)
            )
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
fun GameItem(
    game: Game, 
    onClick: () -> Unit,
    onMomentsTimelineClick: () -> Unit
) {
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
        
        // Dynamic Timeline Button
        IconButton(
            onClick = { onMomentsTimelineClick() },
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF252525), RoundedCornerShape(10.dp))
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "View Timeline",
                tint = Color(0xFF76B947),
                modifier = Modifier.size(20.dp)
            )
        }
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