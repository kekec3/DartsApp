package com.example.darts.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSharingScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Share Game", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                .padding(20.dp)
        ) {
            // Match Summary Card (Matches the visual in image_01e8f3.png)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("John vs Mike", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("May 20, 2024", color = Color.Gray, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("3 - 1", color = Color(0xFF76B947), fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("501 - Best of 5", color = Color.Gray, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Share via Section
            Text("Share via", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShareOptionItem("WhatsApp", Icons.Default.Send, Color(0xFF25D366))
                ShareOptionItem("Messages", Icons.Default.Share, Color(0xFF007AFF))
                ShareOptionItem("Bluetooth", Icons.Default.Share, Color(0xFF1E1E1E))
                ShareOptionItem("Gmail", Icons.Default.Email, Color(0xFFEA4335))
                ShareOptionItem("More", Icons.Default.MoreVert, Color(0xFF1E1E1E))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Export Button
            Button(
                onClick = { /* Export Logic */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "EXPORT GAME FILE",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun ShareOptionItem(label: String, icon: ImageVector, bgColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}