package com.example.darts.ui.screens

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
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
    val config by viewModel.gameSettings.collectAsStateWithLifecycle()
    val isCreating by viewModel.isCreatingGame.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    fun isGpsEnabled() = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    var gpsStatus by remember { mutableStateOf(isGpsEnabled()) }

    val isX01 = config.type.equals("x01", ignoreCase = true)

    // Dropdown visibility states
    var expandedType by remember { mutableStateOf(false) }
    var expandedScore by remember { mutableStateOf(false) }
    var expandedLegs by remember { mutableStateOf(false) }

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
                        text = "Game Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
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
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    // Game Type
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SettingsRow("Game Type", config.type) { expandedType = true }
                        DropdownMenu(
                            expanded = expandedType,
                            onDismissRequest = { expandedType = false },
                            modifier = menuModifier
                        ) {
                            DropdownMenuItem(
                                text = { Text("x01") },
                                colors = menuItemColors,
                                onClick = {
                                    viewModel.updateSettings(
                                        config.copy(
                                            type = "x01",
                                            startingScore = "501",
                                            // reset cricket-only toggles when switching
                                            cutThroat = false
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
                                        config.copy(
                                            type = "cricket",
                                            startingScore = "N/A",
                                            // reset x01-only toggles when switching
                                            doubleOut = false,
                                            masterIn = false
                                        )
                                    )
                                    expandedType = false
                                }
                            )
                        }
                    }

                    SettingsDivider()

                    // Starting Score (x01 only)
                    if (isX01) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val scoreDisplay = if (isX01) config.startingScore else "N/A"
                            SettingsRow("Starting Score", scoreDisplay) {
                                if (isX01) expandedScore = true
                            }
                            if (isX01) {
                                DropdownMenu(
                                    expanded = expandedScore,
                                    onDismissRequest = { expandedScore = false },
                                    modifier = menuModifier
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
                            }
                        }
                    }

                    SettingsDivider()

                    // Legs
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SettingsRow(
                            "Legs",
                            "${config.legs} ${if (config.legs == 1) "Leg" else "Legs"}"
                        ) { expandedLegs = true }
                        DropdownMenu(
                            expanded = expandedLegs,
                            onDismissRequest = { expandedLegs = false },
                            modifier = menuModifier
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
                    }

                    SettingsDivider()

                    // Game-mode-specific toggles
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

                    SettingsDivider()

                    SettingsSwitchRow(
                        label = "Show Animations",
                        checked = config.showAnimations
                    ) { viewModel.updateSettings(config.copy(showAnimations = it)) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (config.trackLocation) {
                if (!locationPermissionState.status.isGranted) {
                    Text(
                        text = "Permission required. Game creation will halt until granted.",
                        color = Color(0xFFE53935),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                } else if (!gpsStatus) {
                    Text(
                        text = "GPS hardware is off. Game creation will route to system settings.",
                        color = Color(0xFFE53935),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                } else {
                    Text(
                        text = "Location tracking ready and verified.",
                        color = Color(0xFF76B947),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            } else {
                Text(
                    text = "Location tracking turned off. Match map data bypassed.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // Bottom Action Bar
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
                modifier = Modifier.clickable { viewModel.updateSettings(GameSettings()) }
            )

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
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(140.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                } else {
                    Text("START", fontWeight = FontWeight.ExtraBold, color = Color.Black)
                }
            }
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
        onComplete = { gameId -> onStartMatch(gameId, config) },
        onError = { errorMsg -> Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show() }
    )
}

@Composable
fun SettingsRow(label: String, value: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
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
    HorizontalDivider(
        color = Color.DarkGray.copy(alpha = 0.3f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}