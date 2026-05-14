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
import com.example.darts.ui.screens.GameCreateScreen
import com.example.darts.ui.screens.GameSettingsScreen
import com.example.darts.ui.screens.HomeScreen
import com.example.darts.ui.screens.PlayersScreen
import com.example.darts.viewModel.BattleViewModel
import com.example.darts.viewModel.GameCreationViewModel
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

        // 1. Home Screen
        composable<HomeRoute> {
            HomeScreen(
                onViewBattles = { navController.navigate(BattlesRoute) },
                onViewPlayers = { navController.navigate(PlayersRoute) },
                onViewStats = { navController.navigate(StatsRoute) }
            )
        }

        // 2. Battles Screen (History/List of all matches)
        composable<BattlesRoute> {
            val viewModel: BattleViewModel = hiltViewModel()
            BattlesScreen(
                battleViewModel = viewModel,
                onBattleClick = { id ->
                    navController.navigate(GameCreateScreenRoute(id))
                },
                addBattle = { navController.navigate(PlayersRoute) }
            )
        }

        // 3. Players Screen (Initial Battle Setup)
        composable<PlayersRoute> {
            val viewModel: BattleViewModel = hiltViewModel()
            PlayersScreen(
                battleViewModel = viewModel,
                onBattleCreated = { id ->
                    navController.navigate(GameCreateScreenRoute(id))
                }
            )
        }

        // 4. Game Lobby Screen (Shows history of legs for a specific battle)
        composable<GameCreateScreenRoute> { backStackEntry ->
            val args: GameCreateScreenRoute = backStackEntry.toRoute()
            val viewModel: GameCreationViewModel = hiltViewModel()

            GameCreateScreen(
                battleId = args.battleId,
                viewModel = viewModel,
                onNewGame = {
                    navController.navigate(GameSettingsRoute(args.battleId))
                },
                onLegSummary = { gameId ->
                    // Navigate to a specific leg summary (if you have one)
                    // navController.navigate(LegSummaryRoute(gameId))
                },
                onMatchSummary = { battleId ->
                    // Navigate to overall battle stats
                    // navController.navigate(StatsRoute(battleId))
                },
                onBack = {
                    // Go back to the list of all battles
                    navController.navigate(BattlesRoute) {
                        popUpTo(HomeRoute) { inclusive = false }
                    }
                }
            )
        }

        // 5. Game Settings Screen (Optional location tagging happens here)
        composable<GameSettingsRoute> { backStackEntry ->
            val args: GameSettingsRoute = backStackEntry.toRoute()
            val viewModel: GameCreationViewModel = hiltViewModel()

            GameSettingsScreen(
                battleId = args.battleId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onStartMatch = { gameId ->
                    // IMPORTANT: We navigate to the game using the newly created Game ID
                    navController.navigate(GameRoute(gameId))
                }
            )
        }

        // 6. Actual Game Screen (The Scoring interface)
        composable<GameRoute> { backStackEntry ->
            val args: GameRoute = backStackEntry.toRoute()
            val viewModel: GameViewModelX01 = hiltViewModel()

            GameScreen(
                viewModel = viewModel,
                gameId = args.gameId
            )
        }
    }
}