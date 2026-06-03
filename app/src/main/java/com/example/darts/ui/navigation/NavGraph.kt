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
import com.example.darts.ui.screens.*
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
                onViewMoments = { navController.navigate(MomentsGalleryRoute) },
                onViewPlayers = { navController.navigate(PlayersRoute(isSelection = false)) },
                onViewStats = { navController.navigate(StatsRoute) }
            )
        }

        // 2. Battles List
        composable<BattlesRoute> {
            BattlesScreen(
                battleViewModel = hiltViewModel(),
                onBattleClick = { id ->
                    navController.navigate(GameCreateScreenRoute(battleId = id))
                },
                addBattle = { navController.navigate(PlayersRoute(isSelection = true)) }
            )
        }

        // 3. Players Screen
        composable<PlayersRoute> { backStackEntry ->
            val args: PlayersRoute = backStackEntry.toRoute()
            PlayersScreen(
                isSelectionMode = args.isSelection,
                battleViewModel = hiltViewModel(),
                onPlayerClick = { id ->
                    navController.navigate(PlayerStatsRoute(playerId = id))
                },
                onBattleCreated = { id ->
                    navController.navigate(GameCreateScreenRoute(battleId = id))
                }
            )
        }

        // 4. Player Stats
        composable<PlayerStatsRoute> { backStackEntry ->
            val args: PlayerStatsRoute = backStackEntry.toRoute()
            PlayerStatsScreen(
                playerId = args.playerId,
                onBack = { navController.popBackStack() }
            )
        }

        // 5. Game Lobby
        composable<GameCreateScreenRoute> { backStackEntry ->
            val args: GameCreateScreenRoute = backStackEntry.toRoute()
            GameCreateScreen(
                battleId = args.battleId,
                viewModel = hiltViewModel(),
                onNewGame = { navController.navigate(GameSettingsRoute(battleId = args.battleId)) },
                onLegSummary = { gId -> navController.navigate(LegSummaryRoute(gameId = gId)) },
                onMatchSummary = { bId -> navController.navigate(MatchSummaryRoute(battleId = bId)) },
                onMomentsTimeline = { gId -> navController.navigate(GameTimelineRoute(gameId = gId)) },
                onBack = { navController.popBackStack() }
            )
        }

        // 6. Game Settings
        composable<GameSettingsRoute> { backStackEntry ->
            val args: GameSettingsRoute = backStackEntry.toRoute()
            GameSettingsScreen(
                battleId = args.battleId,
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onStartMatch = { newGameId, doubleOut, masterIn, maxLegs ->
                    navController.navigate(GameRoute(newGameId, doubleOut, masterIn, maxLegs))
                }
            )
        }

        // 7. Scoring Screen
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

        // 8. Summaries
        composable<LegSummaryRoute> { backStackEntry ->
            val args: LegSummaryRoute = backStackEntry.toRoute()
            LegSummaryScreen(
                gameId = args.gameId,
                onBack = { navController.popBackStack() }
            )
        }

        composable<MatchSummaryRoute> { backStackEntry ->
            val args: MatchSummaryRoute = backStackEntry.toRoute()
            MatchSummaryScreen(
                battleId = args.battleId,
                onBack = { navController.popBackStack() }
            )
        }

        // 9. Moments Gallery
        composable<MomentsGalleryRoute> {
            MomentsGalleryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 10. Timeline Screen
        composable<GameTimelineRoute> { backStackEntry ->
            val args: GameTimelineRoute = backStackEntry.toRoute()
            GameTimelineScreen(
                gameId = args.gameId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}