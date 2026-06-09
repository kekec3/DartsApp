package com.example.darts

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DartsTheme {
                val navController = rememberNavController()

                // Observe the current backstack entry
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // Define which routes SHOULD show the bottom bar
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
                        DartsNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}