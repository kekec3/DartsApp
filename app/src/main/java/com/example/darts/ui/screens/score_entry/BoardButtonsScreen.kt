package com.example.darts.ui.screens.score_entry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
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
fun BoardButtonsScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enter Score", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { /* Back */ }) {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tab Toggle (Numbers / Special)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Numbers", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(24.dp))
                Text("Special", color = Color.Gray, fontSize = 14.sp)
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Number Grid (Left Side)
                Column(
                    modifier = Modifier.weight(3f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val numbers = (1..20).chunked(3)
                    numbers.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { num ->
                                BoardButton(label = num.toString(), modifier = Modifier.weight(1f))
                            }
                            // Fill empty slots for the last row if necessary
                            if (row.size < 3) repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }

                    // Bullseye row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BoardButton(label = "Bull", modifier = Modifier.weight(2.12f)) // Covers two columns
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Row: Undo, Current Score Preview, Backspace
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { }) { Text("UNDO", color = Color.White) }
                        Surface(
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E1E1E)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("0", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Clear, contentDescription = "Delete", tint = Color.White)
                        }
                    }
                }

                // Modifier Column (Right Side)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModifierButton(label = "T", subLabel = "Triple", color = Color(0xFFE57373))
                    ModifierButton(label = "D", subLabel = "Double", color = Color(0xFF81C784))
                    ModifierButton(label = "S", subLabel = "Single", color = Color(0xFF64B5F6))
                }
            }
        }
    }
}

@Composable
fun BoardButton(label: String, modifier: Modifier = Modifier) {
    Surface(
        onClick = { },
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E1E1E)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ColumnScope.ModifierButton(label: String, subLabel: String, color: Color) {
    Surface(
        onClick = { },
        modifier = Modifier.weight(1f).fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E1E1E)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, color = color, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(subLabel, color = color, fontSize = 10.sp)
        }
    }
}