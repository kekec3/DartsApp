package com.example.darts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darts.ui.ScreenBg
import com.example.darts.ui.theme.LimePrimary

enum class QuickGameMode { X01, CRICKET }

@Composable
fun QuickPlaySettingsScreen(
    onBack: () -> Unit,
    onStartGame: (playerCount: Int, mode: QuickGameMode) -> Unit
) {
    var playerCount by remember { mutableIntStateOf(2) }
    var selectedMode by remember { mutableStateOf(QuickGameMode.X01) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Quick Play Setup",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Player Count Selection
            Text(
                text = "PLAYER COUNT",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(2, 3, 4).forEach { count ->
                    val isSelected = playerCount == count
                    Button(
                        onClick = { playerCount = count },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) LimePrimary else Color(0xFF1A201B),
                            contentColor = if (isSelected) Color.Black else Color.White
                        )
                    ) {
                        Text(
                            text = "$count",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Game Mode Selection
            Text(
                text = "GAME MODE",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(QuickGameMode.X01, QuickGameMode.CRICKET).forEach { mode ->
                    val isSelected = selectedMode == mode
                    Button(
                        onClick = { selectedMode = mode },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) LimePrimary else Color(0xFF1A201B),
                            contentColor = if (isSelected) Color.Black else Color.White
                        )
                    ) {
                        Text(
                            text = if (mode == QuickGameMode.X01) "X01 (501)" else "Cricket",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Start Button
            Button(
                onClick = { onStartGame(playerCount, selectedMode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LimePrimary,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "PLAY NOW",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        }
    }
}