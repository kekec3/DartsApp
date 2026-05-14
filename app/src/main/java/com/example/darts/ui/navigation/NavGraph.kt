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
import com.example.darts.ui.screens.LegSummaryScreen
import com.example.darts.ui.screens.MatchSummaryScreen
import com.example.darts.ui.screens.PlayersScreen
// Import your summary screens here
// import com.example.darts.ui.screens.LegSummaryScreen
// import com.example.darts.ui.screens.MatchSummaryScreen

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
                onViewStats = { navController.navigate(StatsRoute) } // Global App Stats
            )
        }

        // 2. Battles Screen
        composable<BattlesRoute> {
            BattlesScreen(
                battleViewModel = hiltViewModel(),
                onBattleClick = { id ->
                    navController.navigate(GameCreateScreenRoute(id))
                },
                addBattle = { navController.navigate(PlayersRoute) }
            )
        }

        // 3. Players Screen
        composable<PlayersRoute> {
            PlayersScreen(
                battleViewModel = hiltViewModel(),
                onBattleCreated = { id ->
                    navController.navigate(GameCreateScreenRoute(id))
                }
            )
        }

        // 4. Game Lobby Screen (Legs List)
        composable<GameCreateScreenRoute> { backStackEntry ->
            val args: GameCreateScreenRoute = backStackEntry.toRoute()

            GameCreateScreen(
                battleId = args.battleId,
                viewModel = hiltViewModel(),
                onNewGame = {
                    navController.navigate(GameSettingsRoute(args.battleId))
                },
                onLegSummary = { gameId ->
                    // Navigate to stats for a single leg
                    navController.navigate(LegSummaryRoute(gameId))
                },
                onMatchSummary = { battleId ->
                    // Navigate to aggregate stats for the whole battle
                    navController.navigate(MatchSummaryRoute(battleId))
                },
                onBack = {
                    navController.navigate(BattlesRoute) {
                        popUpTo(HomeRoute) { inclusive = false }
                    }
                }
            )
        }

        // 5. Game Settings Screen
        composable<GameSettingsRoute> { backStackEntry ->
            val args: GameSettingsRoute = backStackEntry.toRoute()

            GameSettingsScreen(
                battleId = args.battleId,
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onStartMatch = { gameId ->
                    navController.navigate(GameRoute(gameId))
                }
            )
        }

        // 6. Actual Game Screen
        composable<GameRoute> { backStackEntry ->
            val args: GameRoute = backStackEntry.toRoute()
            GameScreen(
                viewModel = hiltViewModel(),
                gameId = args.gameId
            )
        }

        // 7. Leg Summary Screen (Single Game Stats)
        composable<LegSummaryRoute> { backStackEntry ->
            val args: LegSummaryRoute = backStackEntry.toRoute()
            LegSummaryScreen(gameId = args.gameId, onBack = { navController.popBackStack() })
        }

        // 8. Match Summary Screen (Whole Battle Stats)
        composable<MatchSummaryRoute> { backStackEntry ->
            val args: MatchSummaryRoute = backStackEntry.toRoute()
            MatchSummaryScreen(battleId = args.battleId, onBack = { navController.popBackStack() })
        }
    }
}