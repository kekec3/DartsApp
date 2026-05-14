package com.example.darts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun LegSummaryScreen(
    modifier: Modifier = Modifier,
    gameId: Int,
    onBack: () -> Unit,
    onContinue: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            title = { Text("Leg Summary", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Winner and Score Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("John", fontSize = 20.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                        Text("3 - 2", fontSize = 32.sp, fontWeight = FontWeight.Black)
                        Text("Mike", fontSize = 20.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("501 • Best of 5", color = Color.Gray, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Comparison Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                StatComparisonRow("3-Dart Average", "78.45", "72.31", highlightLeft = true)
                StatComparisonRow("First 9 Average", "85.12", "79.23", highlightLeft = true)
                StatComparisonRow("Highest Checkout", "121", "96", highlightLeft = true)
                StatComparisonRow("Checkout %", "42.9%", "33.3%", highlightLeft = true)
                StatComparisonRow("100+ Scores", "8", "6")
                StatComparisonRow("140+ Scores", "3", "2")
                StatComparisonRow("180s", "1", "0")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Continue Button
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
            ) {
                Text("CONTINUE", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun StatComparisonRow(
    label: String,
    leftVal: String,
    rightVal: String,
    highlightLeft: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Value
        Text(
            text = leftVal,
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlightLeft) Color.Green else Color.White,
            textAlign = TextAlign.Start
        )

        // Stat Label
        Text(
            text = label,
            modifier = Modifier.weight(2f),
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        // Right Value
        Text(
            text = rightVal,
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.End
        )
    }
}