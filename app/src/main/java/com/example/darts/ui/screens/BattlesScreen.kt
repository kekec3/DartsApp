package com.example.darts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattlesScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Battles",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Battle",
                            tint = Color.Green,
                            modifier = Modifier.size(32.dp) // Slightly larger icon
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            DartsBottomBar()
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp) // More breathing room
        ) {
            val battles = listOf(
                BattleSummary("John vs Mike", "Last game: May 20, 2024", "3 - 1"),
                BattleSummary("John vs Alex", "Last game: May 18, 2024", "2 - 1"),
                BattleSummary("Team Night", "Last game: May 15, 2024", "3 - 2"),
                BattleSummary("Weekend Darts", "Last game: May 10, 2024", "1 - 3")
            )

            items(battles) { battle ->
                BattleItem(battle)
            }
        }
    }
}

@Composable
fun BattleItem(battle: BattleSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), // More rounded corners
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp) // Increased padding for a "bigger" feel
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = battle.names,
                    fontSize = 22.sp, // Bigger name text
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = battle.date,
                    fontSize = 14.sp, // Slightly bigger date
                    color = Color.Gray
                )
            }
            Text(
                text = battle.score,
                fontSize = 28.sp, // Much bigger score
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
fun DartsBottomBar() {
    NavigationBar(
        containerColor = Color(0xFF121212),
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple("Home", Icons.Default.Home, false),
            Triple("Battles", Icons.Default.PlayArrow, true), // PlayArrow as a proxy for battles
            Triple("Players", Icons.Default.Person, false),
            Triple("More", Icons.Default.Menu, false)
        )

        items.forEach { (label, icon, isSelected) ->
            NavigationBarItem(
                selected = isSelected,
                onClick = { },
                icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp)) },
                label = { Text(label, fontSize = 12.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Green,
                    selectedTextColor = Color.Green,
                    unselectedIconColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

data class BattleSummary(
    val names: String,
    val date: String,
    val score: String
)