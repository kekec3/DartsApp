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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnHistoryScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopAppBar(
            title = {
                Text("Turn History", fontWeight = FontWeight.Bold, color = Color.White)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        // Player Comparison Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HistoryPlayerHeader("John", isActive = true)
            HistoryPlayerHeader("Mike", isActive = false)
        }

        val historyItems = listOf(
            TurnEntry("T20", "60", "421", "501"),
            TurnEntry("S19", "19", "402", "501"),
            TurnEntry("D16", "32", "370", "501"),
            TurnEntry("S10", "10", "360", "501"),
            TurnEntry("T20", "60", "300", "501")
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(historyItems) { entry ->
                HistoryCard(entry)
            }
        }

        // Action Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black
        ) {
            TextButton(
                onClick = { /* Handle Clear */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    "UNDO LAST TURN",
                    color = Color(0xFF76B947),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}

@Composable
fun HistoryCard(entry: TurnEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: The Scoring Hit
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.hit,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF76B947)
                )
                Text(
                    text = "Score: ${entry.points}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }

            // Right: Remaining Comparison
            Row(
                modifier = Modifier.weight(1.5f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.playerOneRemaining,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    text = "|",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = Color.DarkGray,
                    fontSize = 20.sp
                )

                Text(
                    text = entry.playerTwoRemaining,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun HistoryPlayerHeader(name: String, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = name.uppercase(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = if (isActive) Color.White else Color.DarkGray,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .background(
                    color = if (isActive) Color(0xFF76B947) else Color.Transparent,
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}

data class TurnEntry(
    val hit: String,
    val points: String,
    val playerOneRemaining: String,
    val playerTwoRemaining: String
)