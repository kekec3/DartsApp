package com.example.darts.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameImportScreen(
    modifier: Modifier = Modifier
) {
    var isFileSelected by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Import Game", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* Back */ }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isFileSelected) {
                // Upload/Select Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .border(
                            BorderStroke(2.dp, Color.DarkGray),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                        .clickable { isFileSelected = true }, // Simulate file selection
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Select a .darts file to import\nyour match history",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                // Preview of what is being imported
                Text(
                    "Match Preview",
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("John vs Mike", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Importing: 501 Match Data", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Status: Ready to import", color = Color(0xFF76B947), fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Button
            Button(
                onClick = { /* Import Logic */ },
                enabled = isFileSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF76B947),
                    disabledContainerColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "CONFIRM IMPORT",
                    color = if (isFileSelected) Color.Black else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}