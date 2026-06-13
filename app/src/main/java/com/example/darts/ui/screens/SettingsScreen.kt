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
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onImportData:() -> Unit
) {
    var darkMode by remember { mutableStateOf(true) }
    var soundEffects by remember { mutableStateOf(true) }
    var vibration by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopAppBar(
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Back", tint = Color.White)
                }
            },
            actions = {
                // Action icon placeholder or "Save" button
                IconButton(onClick = { /* Add/Save Action */ }) {
                    Icon(Icons.Default.Add, contentDescription = "Action", tint = Color(0xFF76B947))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // --- GENERAL SECTION ---
            SettingsCategoryLabel("GAME DEFAULTS")
            SettingsPlainRow("Starting Score", "501")
            SettingsPlainRow("Match Format", "Best of 5 Legs")
            SettingsPlainRow("Checkout Rule", "Double Out")
            SettingsPlainRow("Starting Player", "Random")

            Spacer(modifier = Modifier.height(24.dp))

            // --- PREFERENCES SECTION ---
            SettingsCategoryLabel("PREFERENCES")
            SettingsSwitchRowPlain("Dark Mode", darkMode) { darkMode = it }
            SettingsSwitchRowPlain("Sound Effects", soundEffects) { soundEffects = it }
            SettingsSwitchRowPlain("Vibration", vibration) { vibration = it }

            Spacer(modifier = Modifier.height(24.dp))

            // --- ACCOUNT & ABOUT ---
            SettingsCategoryLabel("SYSTEM")
            SettingsPlainRow("Import Data", "CSV / JSON", onImportData)
            SettingsPlainRow("Version", "1.0.0")

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SettingsCategoryLabel(label: String) {
    Text(
        text = label,
        color = Color(0xFF76B947),
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsPlainRow(label: String, value: String, onClick: ()->Unit = {}) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isNotEmpty()) {
                    Text(value, color = Color.Gray, fontSize = 14.sp)
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
        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.3f), thickness = 0.5.dp)
    }
}

@Composable
fun SettingsSwitchRowPlain(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 16.sp)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF76B947),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color(0xFF2A2A2A),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.3f), thickness = 0.5.dp)
    }
}