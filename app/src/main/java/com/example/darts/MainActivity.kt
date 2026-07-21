package com.example.darts

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.darts.ui.components.DartsBottomBar
import com.example.darts.ui.navigation.DartsNavGraph
import com.example.darts.ui.theme.DartsTheme
import dagger.hilt.android.AndroidEntryPoint
import com.example.darts.ui.navigation.HomeRoute
import com.example.darts.ui.navigation.BattlesRoute
import com.example.darts.ui.navigation.PlayersRoute
import com.example.darts.ui.navigation.StatsRoute
import com.example.darts.viewModel.GameImportViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val importViewModel: GameImportViewModel by viewModels()

    // Activity Result Launcher to handle the dynamic local-sharing permissions popup
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Filter out exactly which permissions were rejected by the OS
        val deniedPermissions = permissions.filter { !it.value }.keys

        if (deniedPermissions.isNotEmpty()) {
            // Clean up the string so it's easy to read in a Toast
            val missingNames = deniedPermissions.map { it.substringAfterLast(".") }.joinToString(", ")

            Toast.makeText(
                this,
                "OS Denied: $missingNames. Please enable them in system Settings!",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(this, "All local network sharing permissions granted!", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming intent data for Cold Starts (.darts files)
        handleIncomingIntent(intent)

        // Request necessary Bluetooth, Wi-Fi, and Location permissions for Nearby Connections
        checkAndRequestNearbyPermissions()

        setContent {
            DartsTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val showBottomBar = currentDestination?.hasRoute<HomeRoute>() == true ||
                        currentDestination?.hasRoute<BattlesRoute>() == true ||
                        currentDestination?.hasRoute<PlayersRoute>() == true ||
                        currentDestination?.hasRoute<StatsRoute>() == true

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            DartsBottomBar(navController)
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (showBottomBar) innerPadding else PaddingValues(0.dp)),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // Pass the activity-scoped ViewModel down into the Navigation Graph
                        DartsNavGraph(
                            navController = navController,
                            importViewModel = importViewModel
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle incoming intent data for Warm Starts (app running in background)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val data: Uri? = intent.data

        if (Intent.ACTION_VIEW == action && data != null) {
            // Stage the raw file directly. The NavGraph will observe this change and redirect.
            importViewModel.processIncomingFileUri(applicationContext, data)
        }
    }

    // -------------------------------------------------------
    // RUNTIME PERMISSIONS HANDLER FOR NEARBY SHARING
    // -------------------------------------------------------
    private fun checkAndRequestNearbyPermissions() {
        val missingPermissions = getRequiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()

        // 1. Android 12+ requires requesting BOTH coarse and fine location in the same request.
        // On Android 11 and below, location is the baseline fallback requirement.
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        // 2. Android 12 (API 31) and higher requires explicit Bluetooth scanning/connecting
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        // 3. Android 13 (API 33) and higher splits off Wi-Fi from location services entirely
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        return permissions
    }
}