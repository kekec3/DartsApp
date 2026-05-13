package com.example.darts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    var showSuggestions by remember { mutableStateOf(true) }
    var showAnimations by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopAppBar(
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Game Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Back", tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    SettingsRow("Game Type", "501")
                    SettingsDivider()
                    SettingsRow("Starting Score", "501")
                    SettingsDivider()
                    SettingsRow("Legs", "5")
                    SettingsDivider()
                    SettingsRow("Checkout Rule", "Double Out")
                    SettingsDivider()
                    SettingsRow("First to Throw", "Player 1")
                    SettingsDivider()

                    SettingsSwitchRow("Show Suggestions", showSuggestions) { showSuggestions = it }
                    SettingsDivider()
                    SettingsSwitchRow("Show Animations", showAnimations) { showAnimations = it }
                }
            }
        }

        // Action Buttons at the bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RESET TO DEFAULTS",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { /* Reset */ }
            )
            Button(
                onClick = { /* Save */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF76B947)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(140.dp)
            ) {
                Text("SAVE", color = Color.Black, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun SettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF76B947),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
}