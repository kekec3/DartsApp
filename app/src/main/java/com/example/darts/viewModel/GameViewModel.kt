package com.example.darts.viewModel

import com.example.darts.db.entities.MomentType
import com.example.darts.db.repositories.GameRepository
import com.example.darts.engine.DartThrow
import com.example.darts.ui.screens.score_entry.EntryMethod
import com.example.darts.viewModel.states.GameDisplayState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow


sealed interface GameConfig

data class XO1Config(
    val target: Int = 501,
    val doubleOut: Boolean = false,
    val masterIn: Boolean = false,
    val startingPlayerId: Int = -1
) : GameConfig

data class CricketConfig(
    val cutthroat: Boolean = false,
    val startingPlayerId: Int = -1
) : GameConfig

interface BaseGameViewModel {
    val displayState: StateFlow<GameDisplayState>
    val activeEntryMethod: StateFlow<EntryMethod>
    val turnHistory: StateFlow<List<TurnSummary>>

    fun addDart(dart: DartThrow)
    fun undoLastDart()
    fun commitTurn()
    fun setEntryMethod(method: EntryMethod)

    fun captureGameMoment(type: MomentType, contentValue: String)

    val supportedEntryMethods: List<EntryMethod>
        get() = listOf(
            EntryMethod.BoardButtons,
            EntryMethod.ScoreInput,
            EntryMethod.Voice,
            EntryMethod.Camera
        )
    fun loadGame(gameId: Int, maxLegs: Int = 3, config: GameConfig)

    val navigationEvents: SharedFlow<GameNavigationEvent>

    fun consumeNavigationEvent()

    fun getGameRepository() : GameRepository
}

sealed interface GameNavigationEvent {
    data class LegSummary(
        val gameId: Int,
        val legNumber: Int
    ): GameNavigationEvent

    data class MatchSummary(
        val gameId: Int
    ): GameNavigationEvent
}

data class TurnSummary(
    val turnNumber: Int,
    val playerName: String,
    val dartDisplays: List<String>,
    val turnScore: Int,
    val remainingAfter: Int? = null
)