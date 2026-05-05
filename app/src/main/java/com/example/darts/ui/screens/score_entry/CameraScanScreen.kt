package com.example.darts.ui.screens.score_entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
fun CameraScanScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Board", fontWeight = FontWeight.SemiBold) },
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
                .background(Color.Black), // Camera screens usually have a black backdrop
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Viewfinder Frame
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .border(BorderStroke(2.dp, Color.Green), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // The corners in the mockup (2.png) are often more pronounced.
                // You can represent the "scanning" state here.
                Text(
                    "Scanning...",
                    color = Color.Green,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Make sure the board is clearly\nvisible",
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Cancel Button
            Button(
                onClick = { /* Stop Camera */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Text("CANCEL", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}