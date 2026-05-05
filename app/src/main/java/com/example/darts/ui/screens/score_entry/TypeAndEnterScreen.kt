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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeAndEnterScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enter Score", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { /* Back to Game */ }) {
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Type score",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth()
            )

            // Score Display Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(80.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "60", // Placeholder
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Numeric Keypad
            NumericKeypad()

            Spacer(modifier = Modifier.height(24.dp))

            // Large Enter Button
            Button(
                onClick = { /* Submit Score */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
            ) {
                Text(
                    "ENTER",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
            }

            Text(
                "Example: 60\n(Triple 20)",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun NumericKeypad() {
    val buttons = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("-", "0", "DEL")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { label ->
                    KeypadButton(
                        label = label,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun KeypadButton(label: String, modifier: Modifier = Modifier) {
    Surface(
        onClick = { /* Handle Input */ },
        modifier = modifier.aspectRatio(1.5f),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1E1E)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (label == "DEL") {
                Icon(Icons.Default.Clear, contentDescription = "Delete", tint = Color.White)
            } else {
                Text(
                    text = label,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}