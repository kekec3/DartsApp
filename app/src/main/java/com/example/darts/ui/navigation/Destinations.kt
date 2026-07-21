package com.example.darts.ui.navigation

import com.example.darts.db.entities.Game
import kotlinx.serialization.Serializable

@Serializable object SplashRoute
@Serializable object HomeRoute
@Serializable object BattlesRoute
@Serializable object SettingsRoute
@Serializable object StatsRoute
@Serializable object MomentsGalleryRoute
@Serializable object TurnHistoryRoute
@Serializable object GameSharingRoute
@Serializable object GameImportRoute

sealed interface GameRoute

@Serializable
data class X01GameRoute(
    val gameId: Int,
    val maxLegs: Int,
    val target: Int = 501,
    val doubleOut: Boolean = false,
    val masterIn: Boolean = false,
    val startingPlayerId: Int = -1
) : GameRoute

@Serializable
data class MapRoute(
    val battleId: Int? = null
)

@Serializable
data class CricketGameRoute(
    val gameId: Int,
    val maxLegs: Int,
    val cutThroat: Boolean = false,
    val startingPlayerId: Int = -1
) : GameRoute

@Serializable data class GameSettingsRoute(val battleId: Int)
@Serializable data class GameCreateScreenRoute(val battleId: Int)
@Serializable data class LegSummaryRoute(val gameId: Int, val legNumber: Int)
@Serializable data class MatchSummaryRoute(val gameId: Int)
@Serializable data class PlayersRoute(val isSelection: Boolean = false)
@Serializable data class PlayerStatsRoute(val playerId: Int, val playerName: String)
@Serializable data class GameTimelineRoute(val gameId: Int)
