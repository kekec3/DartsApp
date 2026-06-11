package com.example.darts.ui.navigation

import android.os.Build
import android.util.Log
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
import com.example.darts.viewModel.BaseGameViewModel
import com.example.darts.viewModel.CricketConfig
import com.example.darts.viewModel.GameViewModelCricket
import com.example.darts.viewModel.GameViewModelX01
import com.example.darts.viewModel.XO1Config

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
                onViewMoments = {navController.navigate(MomentsGalleryRoute)},
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
                onPlayerClick = { player ->
                    navController.navigate(PlayerStatsRoute(playerId = player.idPlayer, playerName = player.username))
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
                playerName = args.playerName,
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
                onLegSummary = { gameId, legNumber ->
                    navController.navigate(
                        LegSummaryRoute(
                            gameId = gameId,
                            legNumber = legNumber
                        )
                    )
                },
                onMatchSummary = { gameId ->
                    navController.navigate(
                        MatchSummaryRoute(gameId)
                    )
                },
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
                onStartMatch = { gameId, settings ->
                    when (settings.type.lowercase()) {
                        "cricket" -> navController.navigate(
                            CricketGameRoute(
                                gameId = gameId,
                                maxLegs = settings.legs,
                                cutThroat = settings.cutThroat
                            )
                        )
                        else -> navController.navigate(
                            X01GameRoute(
                                gameId = gameId,
                                maxLegs = settings.legs,
                                target = settings.startingScore.toIntOrNull() ?: 501,
                                doubleOut = settings.doubleOut,
                                masterIn = settings.masterIn
                            )
                        )
                    }
                }
            )
        }

        // 7a. X01 Scoring Screen
        composable<X01GameRoute> { backStackEntry ->
            val args: X01GameRoute = backStackEntry.toRoute()
            val config = XO1Config(
                target = args.target,
                doubleOut = args.doubleOut,
                masterIn = args.masterIn
            )
            val viewModel: BaseGameViewModel = hiltViewModel<GameViewModelX01>()
            LaunchedEffect(args.gameId) {
                viewModel.loadGame(
                    gameId = args.gameId,
                    config = config,
                    maxLegs = args.maxLegs
                )
            }
            GameScreen(
                viewModel = viewModel,
                gameId = args.gameId,
                onLegSummary = { gameId, legNumber ->
                    viewModel.consumeNavigationEvent()
                    navController.navigate(
                        LegSummaryRoute(
                            gameId = gameId,
                            legNumber = legNumber
                        )
                    )
                },
                onMatchSummary = { gameId ->
                    viewModel.consumeNavigationEvent()
                    navController.navigate(
                        MatchSummaryRoute(gameId)
                    )
                }
            )
        }

        // 7b. Cricket Scoring Screen
        composable<CricketGameRoute> { backStackEntry ->
            val args: CricketGameRoute = backStackEntry.toRoute()
            val config = CricketConfig(cutthroat = args.cutThroat)
            val viewModel: BaseGameViewModel = hiltViewModel<GameViewModelCricket>()
            LaunchedEffect(args.gameId) {
                viewModel.loadGame(
                    gameId = args.gameId,
                    config = config,
                    maxLegs = args.maxLegs
                )
            }
            GameScreen(
                viewModel = viewModel,
                gameId = args.gameId,
                onLegSummary = { gameId, legNumber ->

                    viewModel.consumeNavigationEvent()
                    navController.navigate(
                        LegSummaryRoute(
                            gameId = gameId,
                            legNumber = legNumber
                        )
                    )
                },
                onMatchSummary = { gameId ->

                    viewModel.consumeNavigationEvent()
                    navController.navigate(
                        MatchSummaryRoute(gameId)
                    )
                }
            )
        }

        // 8. Summaries
        composable<LegSummaryRoute> { backStackEntry ->
            val args: LegSummaryRoute = backStackEntry.toRoute()

            LegSummaryScreen(
                gameId = args.gameId,
                legNumber = args.legNumber,
                onBack = { navController.popBackStack() },
                onContinue = { navController.popBackStack() }
            )
        }

        composable<MatchSummaryRoute> { backStackEntry ->
            val args: MatchSummaryRoute = backStackEntry.toRoute()

            MatchSummaryScreen(
                gameId = args.gameId,
                onBack = { navController.popBackStack() },
                onNavigateHome = {
                    navController.navigate(HomeRoute) {
                        popUpTo<HomeRoute>()
                    }
                }
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

        // 11. Statistics Overview
        composable<StatsRoute> {
            StatisticsOverviewScreen()
        }

        // 12. Settings Screen
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 13. Turn History
        /*composable<TurnHistoryRoute> {
            TurnHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }*/

        // 14. Game Sharing Screen
        composable<GameSharingRoute> {
            GameSharingScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 15. Game Import Screen
        composable<GameImportRoute> {
            GameImportScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}