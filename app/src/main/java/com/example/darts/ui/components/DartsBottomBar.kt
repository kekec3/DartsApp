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
import com.example.darts.ui.navigation.HomeRoute
import com.example.darts.ui.navigation.PlayersRoute
import com.example.darts.ui.navigation.StatsRoute

@Composable
fun DartsBottomBar(navController: NavHostController) {
    // 1. Observe the current navigation state
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 2. Define your items and pair them with their actual Route classes
    val items = listOf(
        Triple("Home", Icons.Default.Home, HomeRoute),
        Triple("Battles", Icons.Default.PlayArrow, BattlesRoute),
        Triple("Players", Icons.Default.Person, PlayersRoute),
        Triple("More", Icons.Default.Menu, StatsRoute)
    )

    NavigationBar(
        containerColor = Color(0xFF121212),
        tonalElevation = 8.dp
    ) {
        items.forEach { (label, icon, route) ->
            // 3. Check if this item's route is currently active in the backstack
            val isSelected = currentDestination?.hierarchy?.any { it.hasRoute(route::class) } == true

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    // 4. Standard navigation logic for bottom bars
                    navController.navigate(route) {
                        // Pop up to the start destination to avoid stack buildup
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same screen
                        launchSingleTop = true
                        // Restore state (like scroll position) when re-selecting
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp)) },
                label = { Text(label, fontSize = 12.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Green,
                    selectedTextColor = Color.Green,
                    unselectedIconColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}