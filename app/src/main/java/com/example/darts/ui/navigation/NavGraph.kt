package com.example.darts.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.darts.db.entities.Game
import com.example.darts.ui.GameScreen
import com.example.darts.ui.screens.*
import com.example.darts.viewModel.BaseGameViewModel
import com.example.darts.viewModel.CricketConfig
import com.example.darts.viewModel.GameImportViewModel
import com.example.darts.viewModel.GameSharingViewModel
import com.example.darts.viewModel.GameViewModelCricket
import com.example.darts.viewModel.GameViewModelX01
import com.example.darts.viewModel.MapViewModel
import com.example.darts.viewModel.XO1Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DartsNavGraph(
    navController: NavHostController = rememberNavController(),
    importViewModel: GameImportViewModel
) {
    val uiState by importViewModel.uiState.collectAsState()

    // AUTO-NAVIGATION ENGINE
    // Whenever parsedPayloadText is populated (not null), jump directly to the Import screen!
    LaunchedEffect(uiState.parsedPayloadText) {
        if (uiState.parsedPayloadText != null) {
            // Customize this string/object to match whatever route key you use for GameImportScreen
            navController.navigate(GameImportRoute) {
                // Optional: Prevents stacking multiple copies of the import screen
                launchSingleTop = true
            }
        }
    }

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
                onViewStats = { navController.navigate(StatsRoute) },
                onViewSettings = {navController.navigate(SettingsRoute)},
                onViewMap = {navController.navigate(MapRoute())}
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
        composable<MapRoute> {
            val viewModel: MapViewModel = hiltViewModel() // ili koinViewModel()
            val games by viewModel.games.collectAsState()

            MapScreen(games = games)
        }

        // 3. Players Screen
        composable<PlayersRoute> { backStackEntry ->
            val args: PlayersRoute = backStackEntry.toRoute()
            PlayersScreen(
                isSelectionMode = args.isSelection,
                battleViewModel = hiltViewModel(),
                onPlayerClick = { player ->
                    navController.navigate(
                        PlayerStatsRoute(playerId = player.idPlayer, playerName = player.username)
                    )
                },
                onBattleCreated = { id ->
                    navController.navigate(GameCreateScreenRoute(battleId = id))
                }
            )
        }

        // 4. Player Stats  ← now uses StatisticsOverviewScreen
        composable<PlayerStatsRoute> { backStackEntry ->
            val args: PlayerStatsRoute = backStackEntry.toRoute()
            StatisticsOverviewScreen(
                playerId = args.playerId,
                playerName = args.playerName,
                onBack     = { navController.popBackStack() }
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
                        LegSummaryRoute(gameId = gameId, legNumber = legNumber)
                    )
                },
                onMatchSummary = { gameId ->
                    navController.navigate(MatchSummaryRoute(gameId))
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
                                gameId   = gameId,
                                maxLegs  = settings.legs,
                                cutThroat = settings.cutThroat,
                                startingPlayerId = settings.startingPlayerId
                            )
                        )
                        else -> navController.navigate(
                            X01GameRoute(
                                gameId    = gameId,
                                maxLegs   = settings.legs,
                                target    = settings.startingScore.toIntOrNull() ?: 501,
                                doubleOut = settings.doubleOut,
                                masterIn  = settings.masterIn,
                                startingPlayerId = settings.startingPlayerId
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
                target    = args.target,
                doubleOut = args.doubleOut,
                masterIn  = args.masterIn,
                startingPlayerId = args.startingPlayerId
            )
            val viewModel: BaseGameViewModel = hiltViewModel<GameViewModelX01>()
            LaunchedEffect(args.gameId) {
                viewModel.loadGame(gameId = args.gameId, config = config, maxLegs = args.maxLegs)
            }
            GameScreen(
                viewModel = viewModel,
                gameId    = args.gameId,
                onLegSummary = { gameId, legNumber ->
                    viewModel.consumeNavigationEvent()
                    navController.navigate(LegSummaryRoute(gameId = gameId, legNumber = legNumber))
                },
                onMatchSummary = { gameId ->
                    viewModel.consumeNavigationEvent()
                    navController.navigate(MatchSummaryRoute(gameId))
                },
                onNavigateBack = {
                    val gameRepository = viewModel.getGameRepository()
                    CoroutineScope(Dispatchers.IO).launch {
                        gameRepository.cleanupUnfinishedGames()
                    }
                    navController.navigate(HomeRoute)
                }

            )
        }

        // 7b. Cricket Scoring Screen
        composable<CricketGameRoute> { backStackEntry ->
            val args: CricketGameRoute = backStackEntry.toRoute()
            val config = CricketConfig(
                cutthroat = args.cutThroat,
                startingPlayerId = args.startingPlayerId
                )
            val viewModel: BaseGameViewModel = hiltViewModel<GameViewModelCricket>()
            LaunchedEffect(args.gameId) {
                viewModel.loadGame(gameId = args.gameId, config = config, maxLegs = args.maxLegs)
            }
            GameScreen(
                viewModel = viewModel,
                gameId    = args.gameId,
                onLegSummary = { gameId, legNumber ->
                    viewModel.consumeNavigationEvent()
                    navController.navigate(LegSummaryRoute(gameId = gameId, legNumber = legNumber))
                },
                onMatchSummary = { gameId ->
                    viewModel.consumeNavigationEvent()
                    navController.navigate(MatchSummaryRoute(gameId))
                },
                onNavigateBack = {
                    val gameRepository = viewModel.getGameRepository()
                    CoroutineScope(Dispatchers.IO).launch {
                        gameRepository.cleanupUnfinishedGames()
                    }
                    navController.navigate(HomeRoute)
                }
            )
        }

        // 8. Summaries
        composable<LegSummaryRoute> { backStackEntry ->
            val args: LegSummaryRoute = backStackEntry.toRoute()
            LegSummaryScreen(
                gameId    = args.gameId,
                legNumber = args.legNumber,
                onBack     = { navController.popBackStack() },
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
            MomentsGalleryScreen(onBack = { navController.popBackStack() })
        }

        // 10. Timeline Screen
        composable<GameTimelineRoute> { backStackEntry ->
            val args: GameTimelineRoute = backStackEntry.toRoute()
            GameTimelineScreen(gameId = args.gameId, onBack = { navController.popBackStack() })
        }

        // 11. Statistics Overview (global, no player context)
        composable<StatsRoute> {
            StatisticsOverviewScreen()
        }

        // 12. Settings Screen
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onImportData = { navController.navigate(GameImportRoute) }
            )
        }

        // 13. Turn History
        /*composable<TurnHistoryRoute> {
            TurnHistoryScreen(onBack = { navController.popBackStack() })
        }*/

        // 14. Game Sharing Screen
        composable<GameSharingRoute> {
            val gameSharingViewModel: GameSharingViewModel = hiltViewModel()
            GameSharingScreen(
                onBack = { navController.popBackStack() },
                viewModel =gameSharingViewModel
            )
        }

        // 15. Game Import Screen
        composable<GameImportRoute> {
            GameImportScreen(
                onBack = {
                    navController.popBackStack()
                },
                viewModel = importViewModel
            )
        }
    }
}