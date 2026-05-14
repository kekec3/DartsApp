package com.example.darts.ui.screens

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun GameSettingsScreen(
    battleId: Int,
    viewModel: GameCreationViewModel,
    onBack: () -> Unit,
    onStartMatch: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Observe the current configuration from the ViewModel
    val config by viewModel.gameSettings.collectAsStateWithLifecycle()

    // Location Permission State
    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )
    val context = LocalContext.current
    val isCreating by viewModel.isCreatingGame.collectAsStateWithLifecycle()
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Check if GPS is enabled
    fun isGpsEnabled() = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

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
                    SettingsRow("Game Type", config.type) { /* TODO: Open Type Picker */ }
                    SettingsDivider()
                    SettingsRow("Starting Score", config.type) { /* Linked to Type */ }
                    SettingsDivider()
                    SettingsRow("Legs", config.legs.toString()) { /* TODO: Open Legs Picker */ }
                    SettingsDivider()
                    SettingsRow("Checkout Rule", config.checkoutRule) { /* TODO: Open Rule Picker */ }
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

                        if (enabled) {

                            if (!locationPermissionState.status.isGranted) {
                                locationPermissionState.launchPermissionRequest()
                            } else {
                                viewModel.updateSettings(
                                    config.copy(trackLocation = true)
                                )
                            }

                        } else {
                            viewModel.updateSettings(
                                config.copy(trackLocation = false)
                            )
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

            // Helpful hint about location
            if (!locationPermissionState.status.isGranted) {
                Text(
                    text = "Location is currently disabled. Games won't be pinned to the map.",
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
                modifier = Modifier.clickable {
                    // Implementation for reset logic
                }
            )
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        // If they come back and GPS is finally on, just start.
                        // Note: We don't auto-start here to avoid confusing the user,
                        // but we let them click the button again which will now work.
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            Button(
                enabled = !isCreating, // Disable button while loading
                onClick = {
                    if (!locationPermissionState.status.isGranted) {

                        // Request permission first
                        locationPermissionState.launchPermissionRequest()

                    } else if (!isGpsEnabled()) {

                        // Ask user to enable GPS
                        context.startActivity(
                            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        )

                    } else {

                        // Permission + GPS OK
                        viewModel.saveAndStartGame(battleId) { id ->
                            onStartMatch(id)
                        }
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
                    Text("START", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun SettingsRow(
    label: String,
    value: String,
    onClick: () -> Unit = {}
) {
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