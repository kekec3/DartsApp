package com.example.darts.ui.screens

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.darts.viewModel.GameCreationViewModel
import com.example.darts.viewModel.GameSettings
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun GameSettingsScreen(
    battleId: Int,
    viewModel: GameCreationViewModel,
    onBack: () -> Unit,
    onStartMatch: (gameId: Int, settings: GameSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    // Load the active battle context
    LaunchedEffect(battleId) {
        viewModel.loadBattle(battleId)
    }

    val config by viewModel.gameSettings.collectAsStateWithLifecycle()
    val isCreating by viewModel.isCreatingGame.collectAsStateWithLifecycle()
    val players by viewModel.battlePlayers.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    fun isGpsEnabled() = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    var gpsStatus by remember { mutableStateOf(isGpsEnabled()) }

    val isX01 = config.type.equals("x01", ignoreCase = true)

    var expandedType by remember { mutableStateOf(false) }
    var expandedScore by remember { mutableStateOf(false) }
    var expandedLegs by remember { mutableStateOf(false) }
    var expandedStartingPlayer by remember { mutableStateOf(false) }

    val menuModifier = Modifier.background(Color(0xFF2A2A2A))
    val menuItemColors = MenuDefaults.itemColors(
        textColor = Color.White,
        leadingIconColor = Color.Gray
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) gpsStatus = isGpsEnabled()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopAppBar(
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "GAME SETTINGS",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(20.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, Color.DarkGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column {
                    SettingsDropdownRow(
                        label = "Game Type",
                        value = config.type,
                        expanded = expandedType,
                        onExpandedChange = { expandedType = it },
                        menuModifier = menuModifier,
                        menuItemColors = menuItemColors
                    ) {
                        DropdownMenuItem(
                            text = { Text("x01") },
                            colors = menuItemColors,
                            onClick = {
                                viewModel.updateSettings(
                                    config.copy(
                                        type = "x01",
                                        startingScore = if (config.startingScore == "N/A") "501" else config.startingScore
                                    )
                                )
                                expandedType = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cricket") },
                            colors = menuItemColors,
                            onClick = {
                                viewModel.updateSettings(
                                    config.copy(type = "cricket")
                                )
                                expandedType = false
                            }
                        )
                    }

                    SettingsDivider()

                    if (isX01) {
                        SettingsDropdownRow(
                            label = "Starting Score",
                            value = config.startingScore,
                            expanded = expandedScore,
                            onExpandedChange = { expandedScore = it },
                            menuModifier = menuModifier,
                            menuItemColors = menuItemColors
                        ) {
                            listOf("301", "501", "701").forEach { score ->
                                DropdownMenuItem(
                                    text = { Text(score) },
                                    colors = menuItemColors,
                                    onClick = {
                                        viewModel.updateSettings(config.copy(startingScore = score))
                                        expandedScore = false
                                    }
                                )
                            }
                        }
                        SettingsDivider()
                    }

                    SettingsDropdownRow(
                        label = "Legs",
                        value = "${config.legs} ${if (config.legs == 1) "Leg" else "Legs"}",
                        expanded = expandedLegs,
                        onExpandedChange = { expandedLegs = it },
                        menuModifier = menuModifier,
                        menuItemColors = menuItemColors
                    ) {
                        listOf(1, 3, 5, 7, 9).forEach { legCount ->
                            DropdownMenuItem(
                                text = { Text("$legCount ${if (legCount == 1) "Leg" else "Legs"}") },
                                colors = menuItemColors,
                                onClick = {
                                    viewModel.updateSettings(config.copy(legs = legCount))
                                    expandedLegs = false
                                }
                            )
                        }
                    }

                    SettingsDivider()

                    // --- Dynamic Starting Player Selection Dropdown ---
                    val startingPlayerLabel = when (config.startingPlayerId) {
                        -1 -> "Random"
                        -2 -> "Default (First)"
                        else -> players.find { it.idPlayer == config.startingPlayerId }?.username ?: "Default"
                    }

                    SettingsDropdownRow(
                        label = "Starting Player",
                        value = startingPlayerLabel,
                        expanded = expandedStartingPlayer,
                        onExpandedChange = { expandedStartingPlayer = it },
                        menuModifier = menuModifier,
                        menuItemColors = menuItemColors
                    ) {
                        DropdownMenuItem(
                            text = { Text("Random") },
                            colors = menuItemColors,
                            onClick = {
                                viewModel.updateSettings(config.copy(startingPlayerId = -1))
                                expandedStartingPlayer = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Default (First)") },
                            colors = menuItemColors,
                            onClick = {
                                viewModel.updateSettings(config.copy(startingPlayerId = -2))
                                expandedStartingPlayer = false
                            }
                        )
                        players.forEach { player ->
                            DropdownMenuItem(
                                text = { Text(player.username) },
                                colors = menuItemColors,
                                onClick = {
                                    viewModel.updateSettings(config.copy(startingPlayerId = player.idPlayer))
                                    expandedStartingPlayer = false
                                }
                            )
                        }
                    }

                    SettingsDivider()

                    if (isX01) {
                        SettingsSwitchRow(
                            label = "Double Out",
                            checked = config.doubleOut
                        ) { viewModel.updateSettings(config.copy(doubleOut = it)) }
                        SettingsDivider()
                        SettingsSwitchRow(
                            label = "Master In",
                            checked = config.masterIn
                        ) { viewModel.updateSettings(config.copy(masterIn = it)) }
                    } else {
                        SettingsSwitchRow(
                            label = "Cut-Throat",
                            checked = config.cutThroat
                        ) { viewModel.updateSettings(config.copy(cutThroat = it)) }
                    }

                    SettingsDivider()

                    SettingsSwitchRow(
                        label = "Show Suggestions",
                        checked = config.showSuggestions
                    ) { viewModel.updateSettings(config.copy(showSuggestions = it)) }

                    SettingsDivider()

                    SettingsSwitchRow(
                        label = "Track Match Location",
                        checked = config.trackLocation
                    ) { enabled ->
                        viewModel.updateSettings(config.copy(trackLocation = enabled))
                        if (enabled && !locationPermissionState.status.isGranted) {
                            locationPermissionState.launchPermissionRequest()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LocationStatusBox(
                trackLocation = config.trackLocation,
                isGranted = locationPermissionState.status.isGranted,
                gpsStatus = gpsStatus
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { viewModel.resetToDefaults() },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
            ) {
                Text(
                    text = "RESET DEFAULTS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Button(
                enabled = !isCreating,
                onClick = {
                    if (config.trackLocation) {
                        if (!locationPermissionState.status.isGranted) {
                            locationPermissionState.launchPermissionRequest()
                        } else if (!isGpsEnabled()) {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } else {
                            runGameCreation(battleId, config, viewModel, onStartMatch, context)
                        }
                    } else {
                        runGameCreation(battleId, config, viewModel, onStartMatch, context)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCreating) Color.Gray else Color(0xFF76B947)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.width(160.dp).height(48.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 3.dp)
                } else {
                    Text("START MATCH", fontWeight = FontWeight.ExtraBold, color = Color.Black, fontSize = 14.sp)
                }
            }
        }
    }
}

// --- Internal Screen Sub-Composables & Helpers ---

@Composable
fun SettingsDropdownRow(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    menuModifier: Modifier,
    menuItemColors: MenuItemColors,
    content: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandedChange(true) }
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = menuModifier
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF76B947),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF2D2D2D),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        color = Color.DarkGray.copy(alpha = 0.3f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun LocationStatusBox(
    trackLocation: Boolean,
    isGranted: Boolean,
    gpsStatus: Boolean
) {
    if (trackLocation) {
        val (statusText, statusColor) = when {
            !isGranted -> "Location Permission Denied" to Color.Red
            !gpsStatus -> "GPS is Disabled" to Color.Yellow
            else -> "Location tracking active (GPS)" to Color(0xFF76B947)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun runGameCreation(
    battleId: Int,
    config: GameSettings,
    viewModel: GameCreationViewModel,
    onStartMatch: (Int, GameSettings) -> Unit,
    context: Context
) {
    viewModel.saveAndStartGame(
        battleId = battleId,
        onComplete = { gameId ->
            onStartMatch(gameId, config)
        },
        onError = { errorMsg ->
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
        }
    )
}