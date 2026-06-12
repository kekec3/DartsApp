package com.example.darts.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.darts.ui.navigation.BattlesRoute
import com.example.darts.ui.navigation.GameSharingRoute
import com.example.darts.ui.navigation.HomeRoute
import com.example.darts.ui.navigation.PlayersRoute
import com.example.darts.ui.navigation.StatsRoute

@Composable
fun DartsBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = Color(0xFF121212),
        tonalElevation = 8.dp
    ) {
        // --- HOME ITEM ---
        NavigationBarItem(
            selected = currentDestination?.hierarchy?.any { it.hasRoute<HomeRoute>() } == true,
            onClick = {
                navController.navigate(HomeRoute) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true; saveState = false }
                    launchSingleTop = true
                    restoreState = false
                }
            },
            icon = { Icon(Icons.Default.Home, null, modifier = Modifier.size(26.dp)) },
            label = { Text("Home", fontSize = 12.sp) },
            colors = navigationItemColors()
        )

        // --- BATTLES ITEM ---
        NavigationBarItem(
            selected = currentDestination?.hierarchy?.any { it.hasRoute<BattlesRoute>() } == true,
            onClick = {
                navController.navigate(BattlesRoute) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(26.dp)) },
            label = { Text("Battles", fontSize = 12.sp) },
            colors = navigationItemColors()
        )

        // --- PLAYERS ITEM ---
        NavigationBarItem(
            selected = currentDestination?.hierarchy?.any { it.hasRoute<PlayersRoute>() } == true,
            onClick = {
                navController.navigate(PlayersRoute(isSelection = false)) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(26.dp)) },
            label = { Text("Players", fontSize = 12.sp) },
            colors = navigationItemColors()
        )

        // --- MORE/STATS ITEM ---
        NavigationBarItem(
            selected = currentDestination?.hierarchy?.any { it.hasRoute<GameSharingRoute>() } == true,
            onClick = {
                navController.navigate(GameSharingRoute) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.Menu, null, modifier = Modifier.size(26.dp)) },
            label = { Text("More", fontSize = 12.sp) },
            colors = navigationItemColors()
        )
    }
}

@Composable
fun navigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Color.Green,
    selectedTextColor = Color.Green,
    unselectedIconColor = Color.Gray,
    indicatorColor = Color.Transparent
)