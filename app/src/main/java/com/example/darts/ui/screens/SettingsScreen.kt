package com.example.darts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    var darkMode by remember { mutableStateOf(true) }
    var soundEffects by remember { mutableStateOf(true) }
    var vibration by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* Back */ }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Add/Action */ }) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        bottomBar = { DartsBottomBar() } // Ensure this uses the green "Settings" active state
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.Black)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // --- GENERAL SECTION ---
            SettingsCategoryLabel("GENERAL")
            SettingsPlainRow("Default Game", "501")
            SettingsPlainRow("Default Match Format", "Best of 5")
            SettingsPlainRow("Checkout Rule", "Double Out")
            SettingsPlainRow("First to Throw", "Player 1")

            Spacer(modifier = Modifier.height(24.dp))

            // --- PREFERENCES SECTION ---
            SettingsCategoryLabel("PREFERENCES")
            SettingsSwitchRowPlain("Dark Mode", darkMode) { darkMode = it }
            SettingsSwitchRowPlain("Sound Effects", soundEffects) { soundEffects = it }
            SettingsSwitchRowPlain("Vibration", vibration) { vibration = it }

            Spacer(modifier = Modifier.height(24.dp))

            // --- ABOUT SECTION ---
            SettingsCategoryLabel("ABOUT")
            SettingsPlainRow("About Dart Score", "")
            SettingsPlainRow("Version", "1.0.0")

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SettingsCategoryLabel(label: String) {
    Text(
        text = label,
        color = Color.Gray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
fun SettingsPlainRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* Navigate */ }
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isNotEmpty()) {
                    Text(value, color = Color.Gray, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)
    }
}

@Composable
fun SettingsSwitchRowPlain(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 16.sp)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF76B947), // Matches the green in the image
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color(0xFF333333)
                )
            )
        }
        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)
    }
}