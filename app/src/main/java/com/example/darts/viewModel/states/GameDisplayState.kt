package com.example.darts.viewModel.states

import com.example.darts.engine.DartThrow

data class StatRow(
    val label: String,
    val value: String
)

data class PlayerDisplayState(
    val id: String,
    val name: String,
    val primaryScore: String,
    val legsWon: Int,
    val stats: List<StatRow>,
    val isCurrent: Boolean
)

data class DartSlotState(
    val dart: DartThrow?,
    val index: Int
)

data class TurnDisplayState(
    val slots: List<DartSlotState> = List(3) { DartSlotState(null, it) },
    val turnScore: Int = 0,
    val remaining: Int? = null,
    val isBust: Boolean = false,
    val currentPlayerName: String = "",
    val dartsEnteredCount: Int = 0
)

data class GameDisplayState(
    val gameTitle: String = "",
    val players: List<PlayerDisplayState> = emptyList(),
    val turn: TurnDisplayState = TurnDisplayState(),
    val isFinished: Boolean = false,
    val winner: String? = null
)

/**
 * Cricket-specific projection for the mark grid, exposed by GameViewModelCricket
 * alongside [GameDisplayState] — which has no concept of marks.
 */
data class CricketUiState(
    val playerStates: List<PlayerStateCricket> = emptyList(),
    val currentPlayerIndex: Int = 0
)