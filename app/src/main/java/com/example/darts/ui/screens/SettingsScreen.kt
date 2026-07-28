package com.example.darts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.darts.viewModel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onImportData: () -> Unit
) {
    val gameType by viewModel.gameType.collectAsStateWithLifecycle()
    val startingScore by viewModel.startingScore.collectAsStateWithLifecycle()
    val legs by viewModel.legs.collectAsStateWithLifecycle()
    val doubleOut by viewModel.doubleOut.collectAsStateWithLifecycle()
    val masterIn by viewModel.masterIn.collectAsStateWithLifecycle()
    val cutThroat by viewModel.cutThroat.collectAsStateWithLifecycle()
    val showSuggestions by viewModel.showSuggestions.collectAsStateWithLifecycle()
    val trackLocation by viewModel.trackLocation.collectAsStateWithLifecycle()
    val startingPlayerDefault by viewModel.startingPlayerDefault.collectAsStateWithLifecycle()
    val soundEffects by viewModel.soundEffects.collectAsStateWithLifecycle()

    val isX01 = gameType.equals("x01", ignoreCase = true)

    var expandedType by remember { mutableStateOf(false) }
    var expandedScore by remember { mutableStateOf(false) }
    var expandedLegs by remember { mutableStateOf(false) }
    var expandedStartingPlayer by remember { mutableStateOf(false) }

    val menuItemColors = MenuDefaults.itemColors(
        textColor = Color.White,
        leadingIconColor = Color.Gray
    )

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
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // --- GAME DEFAULTS SECTION ---
            SettingsCategoryLabel("GAME DEFAULTS")

            // Game Type Selector
            SettingsDropdownRow(
                label = "Game Type",
                value = if (isX01) "x01" else "Cricket",
                expanded = expandedType,
                onExpandedChange = { expandedType = it }
            ) {
                DropdownMenuItem(
                    text = { Text("x01") },
                    colors = menuItemColors,
                    onClick = {
                        viewModel.setGameType("x01")
                        expandedType = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Cricket") },
                    colors = menuItemColors,
                    onClick = {
                        viewModel.setGameType("Cricket")
                        expandedType = false
                    }
                )
            }

            // X01 Specific: Starting Score
            if (isX01) {
                SettingsDropdownRow(
                    label = "Starting Score",
                    value = startingScore,
                    expanded = expandedScore,
                    onExpandedChange = { expandedScore = it }
                ) {
                    listOf("301", "501", "701").forEach { score ->
                        DropdownMenuItem(
                            text = { Text(score) },
                            colors = menuItemColors,
                            onClick = {
                                viewModel.setStartingScore(score)
                                expandedScore = false
                            }
                        )
                    }
                }
            }

            // Common: Match Format
            SettingsDropdownRow(
                label = "Match Format",
                value = if (legs == 1) "1 Leg" else "Best of $legs Legs",
                expanded = expandedLegs,
                onExpandedChange = { expandedLegs = it }
            ) {
                listOf(1, 3, 5, 7, 9).forEach { legCount ->
                    DropdownMenuItem(
                        text = { Text(if (legCount == 1) "1 Leg" else "Best of $legCount Legs") },
                        colors = menuItemColors,
                        onClick = {
                            viewModel.setLegs(legCount)
                            expandedLegs = false
                        }
                    )
                }
            }

            // Common: Starting Player Default
            SettingsDropdownRow(
                label = "Starting Player",
                value = if (startingPlayerDefault.lowercase() == "random") "Random" else "Default (First)",
                expanded = expandedStartingPlayer,
                onExpandedChange = { expandedStartingPlayer = it }
            ) {
                DropdownMenuItem(
                    text = { Text("Random") },
                    colors = menuItemColors,
                    onClick = {
                        viewModel.setStartingPlayerDefault("Random")
                        expandedStartingPlayer = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Default (First)") },
                    colors = menuItemColors,
                    onClick = {
                        viewModel.setStartingPlayerDefault("Default")
                        expandedStartingPlayer = false
                    }
                )
            }

            // Mode-specific Switches
            if (isX01) {
                SettingsSwitchRowPlain("Double Out", doubleOut) { viewModel.setDoubleOut(it) }
                SettingsSwitchRowPlain("Master In", masterIn) { viewModel.setMasterIn(it) }
            } else {
                SettingsSwitchRowPlain("Cut-Throat", cutThroat) { viewModel.setCutThroat(it) }
            }

            // General Game Switches
            SettingsSwitchRowPlain("Show Suggestions", showSuggestions) { viewModel.setShowSuggestions(it) }
            SettingsSwitchRowPlain("Track Match Location", trackLocation) { viewModel.setTrackLocation(it) }

            Spacer(modifier = Modifier.height(24.dp))

            // --- PREFERENCES SECTION ---
            SettingsCategoryLabel("PREFERENCES")
            SettingsSwitchRowPlain("Sound Effects", soundEffects) { viewModel.setSoundEffects(it) }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SYSTEM SECTION ---
            SettingsCategoryLabel("SYSTEM")
            SettingsPlainRow("Import Data", "CSV / JSON", onClick = onImportData)
            SettingsPlainRow("Version", "1.0.0")

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SettingsDropdownRow(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(true) }
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 16.sp)
            Box {
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
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) },
                    modifier = Modifier.background(Color(0xFF2A2A2A))
                ) {
                    content()
                }
            }
        }
        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.3f), thickness = 0.5.dp)
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
fun SettingsPlainRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Column {
        val clickableModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(clickableModifier)
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