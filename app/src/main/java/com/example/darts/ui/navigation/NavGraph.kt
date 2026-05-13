package com.example.darts.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.darts.ui.GameScreen
import com.example.darts.ui.screens.BattlesScreen
import com.example.darts.ui.screens.HomeScreen
import com.example.darts.ui.screens.PlayersScreen
import com.example.darts.viewModel.BattleViewModel
import com.example.darts.viewModel.GameViewModelX01


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DartsNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute
    ) {

        // 2. Home Screen (Central Hub)
        composable<HomeRoute> {
            HomeScreen(
                onViewBattles = { navController.navigate(BattlesRoute) },
                onViewPlayers = { navController.navigate(PlayersRoute) },
                onViewStats = { navController.navigate(StatsRoute) }
            )
        }

        // 3. Battles Screen (Uses BattleViewModel)
        composable<BattlesRoute> {
            val viewModel: BattleViewModel = hiltViewModel()
            BattlesScreen(
                battleViewModel = viewModel,
                onBattleClick = { id -> navController.navigate(GameRoute(id)) },
                addBattle = {navController.navigate(PlayersRoute)}
            )
        }

        // 13. Players Screen (Uses shared BattleViewModel logic)
        composable<PlayersRoute> {
            val viewModel: BattleViewModel = hiltViewModel()
            PlayersScreen(
                battleViewModel = viewModel,
                onBattleCreated = { id ->
                    navController.navigate(GameRoute(id))
                }
            )
        }

        // 4. Game Screen
        composable<GameRoute> { backStackEntry ->
            val viewModel: GameViewModelX01 = hiltViewModel()
            val args: GameRoute = backStackEntry.toRoute()
            GameScreen(viewModel = viewModel,battleId = args.battleId)
        }
    }
}