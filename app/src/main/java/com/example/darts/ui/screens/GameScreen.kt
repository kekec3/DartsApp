package com.example.darts.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
fun GameScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("John vs Mike", style = MaterialTheme.typography.titleMedium)
                        Text("Leg 1 / 5", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* Back */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Score Board
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerScoreColumn("John", "321", isActive = true)
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(60.dp)
                        .background(Color.DarkGray)
                )
                PlayerScoreColumn("Mike", "278", isActive = false)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Turn Label
            Text(
                "John's Turn",
                color = Color.Green,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            // Dartboard Placeholder
            // In a real app, you'd use a custom Canvas or Image here.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(280.dp),
                    shape = CircleShape,
                    color = Color(0xFF1E1E1E),
                    border = BorderStroke(4.dp, Color.DarkGray)
                ) {
                    // This represents the dartboard from 2.png
                    Box(contentAlignment = Alignment.Center) {
                        Text("Dartboard Visual", color = Color.DarkGray)
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GameActionButton("UNDO", Color.DarkGray, Modifier.weight(1f))
                IconButton(
                    onClick = { /* History */ },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFF1E1E1E), CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "History", tint = Color.White)
                }
                GameActionButton("SCORE", Color.Green, Modifier.weight(1f), textColor = Color.Black)
            }
        }
    }
}

@Composable
fun PlayerScoreColumn(name: String, score: String, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Green, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isActive) Color.White else Color.Gray
            )
        }
        Text(
            score,
            fontSize = 54.sp,
            fontWeight = FontWeight.Black,
            color = if (isActive) Color.Green else Color.White
        )
    }
}

@Composable
fun GameActionButton(
    label: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White
) {
    Button(
        onClick = { /* Action */ },
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor)
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
    }
}