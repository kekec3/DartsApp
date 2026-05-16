package com.example.darts.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object SplashRoute
@Serializable object HomeRoute
@Serializable object BattlesRoute
@Serializable object PlayersRoute
@Serializable object SettingsRoute
@Serializable object StatsRoute

@Serializable
data class GameRoute(val gameId: Int, val doubleOut: Boolean = false, val masterIn: Boolean = false, val maxLegs: Int = 3)

@Serializable
data class GameSettingsRoute(val battleId: Int)
@Serializable data class GameCreateScreenRoute(val battleId: Int)@Serializable data class LegSummaryRoute(val gameId: Int)    // New
@Serializable data class MatchSummaryRoute(val battleId: Int) // New