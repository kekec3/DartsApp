package com.example.darts

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming intent data for Cold Starts
        handleIncomingIntent(intent)

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
}