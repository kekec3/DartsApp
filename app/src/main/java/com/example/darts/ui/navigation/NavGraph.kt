package com.example.darts.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.darts.viewModel.GameViewModelX01

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
        // 1. Home
        composable<HomeRoute> {
            HomeScreen(
                onViewBattles = { navController.navigate(BattlesRoute) },
                onViewPlayers = { navController.navigate(PlayersRoute) },
                onViewStats = { navController.navigate(StatsRoute) }
            )
        }

        // 2. Battles List
        composable<BattlesRoute> {
            BattlesScreen(
                battleViewModel = hiltViewModel(),
                onBattleClick = { id -> navController.navigate(GameCreateScreenRoute(id)) },
                addBattle = { navController.navigate(PlayersRoute) }
            )
        }

        // 3. Players Setup
        composable<PlayersRoute> {
            PlayersScreen(
                battleViewModel = hiltViewModel(),
                onBattleCreated = { id -> navController.navigate(GameCreateScreenRoute(id)) }
            )
        }

        // 4. Game Lobby (The history of legs for this battle)
        composable<GameCreateScreenRoute> { backStackEntry ->
            val args: GameCreateScreenRoute = backStackEntry.toRoute()
            GameCreateScreen(
                battleId = args.battleId,
                viewModel = hiltViewModel(),
                onNewGame = { navController.navigate(GameSettingsRoute(args.battleId)) },
                onLegSummary = { gId -> navController.navigate(LegSummaryRoute(gId)) },
                onMatchSummary = { bId -> navController.navigate(MatchSummaryRoute(bId)) },
                onBack = { navController.popBackStack() }
            )
        }

        // 5. Game Settings (Where the location is pinned and game is created)
        composable<GameSettingsRoute> { backStackEntry ->
            val args: GameSettingsRoute = backStackEntry.toRoute()
            GameSettingsScreen(
                battleId = args.battleId,
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onStartMatch = { newGameId, doubleOut, masterIn, maxLegs ->
                    // Navigate using the ID of the leg we just created
                    navController.navigate(GameRoute(newGameId, doubleOut, masterIn, maxLegs))
                }
            )
        }

        // 6. THE FIX: Restored GameViewModelX01 for the scoring screen
        composable<GameRoute> { backStackEntry ->
            val args: GameRoute = backStackEntry.toRoute()
            val viewModel: GameViewModelX01 = hiltViewModel()

            LaunchedEffect(args.gameId) {
                viewModel.loadGame(args.gameId, args.doubleOut, args.masterIn, args.maxLegs)
            }

            GameScreen(
                viewModel = viewModel,
                gameId = args.gameId
            )
        }

        // 7. Leg Summary
        composable<LegSummaryRoute> { backStackEntry ->
            val args: LegSummaryRoute = backStackEntry.toRoute()
            LegSummaryScreen(gameId = args.gameId, onBack = { navController.popBackStack() })
        }

        composable<MatchSummaryRoute> { backStackEntry ->
            val args: MatchSummaryRoute = backStackEntry.toRoute()
            MatchSummaryScreen(battleId = args.battleId, onBack = { navController.popBackStack() })
        }
    }
}