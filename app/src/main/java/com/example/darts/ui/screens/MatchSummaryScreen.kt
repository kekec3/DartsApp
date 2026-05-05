package com.example.darts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star // Using Star as a proxy for Trophy
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
fun MatchSummaryScreen(
    modifier: Modifier = Modifier
) {
    Scaffold { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Trophy / Winner Icon
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = Color(0xFF2E7D32)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Winner",
                        modifier = Modifier.size(50.dp),
                        tint = Color.Green
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Match Result Header
            Text(
                text = "John Wins!",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                text = "3 - 1",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = "501 • Best of 5",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Stats Comparison Grid
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                MatchStatRow("3-Dart Average", "82.14", "75.21", highlightLeft = true)
                MatchStatRow("Checkout %", "45.5%", "31.3%", highlightLeft = true)
                MatchStatRow("Highest Checkout", "118", "96", highlightLeft = true)
                MatchStatRow("180s", "2", "0", highlightLeft = true)
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(40.dp))

            // Back to Home Button
            Button(
                onClick = { /* Navigate Home */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
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
fun MatchStatRow(
    label: String,
    leftVal: String,
    rightVal: String,
    highlightLeft: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = leftVal,
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlightLeft) Color.Green else Color.White,
            textAlign = TextAlign.Start
        )

        Text(
            text = label,
            modifier = Modifier.weight(2f),
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Text(
            text = rightVal,
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.End
        )
    }
}