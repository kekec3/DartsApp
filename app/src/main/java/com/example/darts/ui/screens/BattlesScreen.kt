package com.example.darts.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.darts.db.entities.Battle
import com.example.darts.viewModel.BattleViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattlesScreen(
    modifier: Modifier = Modifier,
    battleViewModel: BattleViewModel = hiltViewModel()
) {
    val battles by battleViewModel.allBattles.collectAsState()

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

            items(battles) { battle ->
                BattleItem(battle)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BattleItem(battle: Battle) {
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
                    text = battle.name,
                    fontSize = 22.sp, // Bigger name text
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatBattleDate(battle.dateCreated),
                    fontSize = 14.sp, // Slightly bigger date
                    color = Color.Gray
                )
            }
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

@RequiresApi(Build.VERSION_CODES.O)
fun formatBattleDate(timestamp: String): String {
    return try {
        val millis = timestamp.toLong()
        val instant = Instant.ofEpochMilli(millis)
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        "N/A"
    }
}