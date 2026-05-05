package com.example.darts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnHistoryScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Turn History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Player Comparison Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HistoryPlayerHeader("John", isActive = true)
                HistoryPlayerHeader("Mike", isActive = false)
            }

            val historyItems = listOf(
                TurnEntry("T20", "60", "421"),
                TurnEntry("S19", "19", "402"),
                TurnEntry("D16", "32", "370"),
                TurnEntry("S10", "10", "360"),
                TurnEntry("T20", "60", "300")
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyItems) { entry ->
                    HistoryCard(entry)
                }
            }

            // Bottom Action
            TextButton(
                onClick = { /* TODO */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    "CLEAR HISTORY",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun HistoryCard(entry: TurnEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: The hit (e.g., T20)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.hit,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Green
                )
                Text(
                    text = "Score: ${entry.playerOneRemaining}", // Example of detail we can add
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Right: Comparison of Remaining Scores
            Row(
                modifier = Modifier.weight(1.5f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.playerOneRemaining,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "/",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = Color.DarkGray,
                    fontSize = 20.sp
                )

                Text(
                    text = entry.playerTwoRemaining,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun HistoryPlayerHeader(name: String, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = name,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isActive) Color.White else Color.Gray
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(3.dp)
                .background(if (isActive) Color.Green else Color.Transparent, RoundedCornerShape(2.dp))
        )
    }
}

data class TurnEntry(
    val hit: String,
    val playerOneRemaining: String,
    val playerTwoRemaining: String
)