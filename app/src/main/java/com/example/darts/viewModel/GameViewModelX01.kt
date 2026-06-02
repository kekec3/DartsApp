package com.example.darts.viewModel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.db.entities.Moment
import com.example.darts.db.entities.MomentType
import com.example.darts.db.entities.Player
import com.example.darts.db.repositories.BattleRepository
import com.example.darts.db.repositories.GameRepository
import com.example.darts.db.repositories.MomentRepository
import com.example.darts.engine.DartThrow
import com.example.darts.engine.GameEngineX01
import com.example.darts.engine.Multiplier
import com.example.darts.engine.Turn
import com.example.darts.ui.screens.score_entry.EntryMethod
import com.example.darts.utils.SoundManager
import com.example.darts.viewModel.states.DartSlotState
import com.example.darts.viewModel.states.GameDisplayState
import com.example.darts.viewModel.states.PlayerDisplayState
import com.example.darts.viewModel.states.PlayerStateX01
import com.example.darts.viewModel.states.StatRow
import com.example.darts.viewModel.states.TurnDisplayState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModelX01 @Inject constructor(
    private val gameRepository: GameRepository,
    private val battleRepository: BattleRepository,
    private val momentRepository: MomentRepository,
    private val soundManager: SoundManager
) : ViewModel(), BaseGameViewModel {

    private lateinit var engine: GameEngineX01
    private var gameId: Int = -1

    fun startGame(players: List<PlayerStateX01>, target: Int = 501, doubleOut: Boolean = false, masterIn: Boolean = false, maxLegs: Int = 3) {
        engine = GameEngineX01(players, target, doubleOut, masterIn, maxLegs)
        refresh()

        viewModelScope.launch {
            kotlinx.coroutines.delay(200)
            soundManager.playGameOn()
        }
    }

    fun loadGame(gameId: Int, doubleOut: Boolean = false, masterIn: Boolean = false, maxLegs: Int = 3) {
        this.gameId = gameId
        viewModelScope.launch {
            val game = gameRepository.getGameById(gameId) ?: return@launch
            val players = battleRepository.getPlayersOfBattle(game.idBattle)
            val playerStates = players.map { PlayerStateX01(player = it) }
            val target = game.type.toIntOrNull() ?: 501
            startGame(playerStates, target, doubleOut, masterIn, maxLegs)
        }
    }

    override fun captureGameMoment(type: MomentType, contentValue: String) {
        viewModelScope.launch {
            Log.d("MomentCapture", "Capturing $type with value: $contentValue for gameId: $gameId")
            if (gameId != -1) {
                momentRepository.saveMoment(Moment(idGame = gameId, type = type, contentValue = contentValue))
            } else {
                Log.e("MomentCapture", "GameId is -1! Could not save moment.")
            }
        }
    }

    private val currentDarts = mutableListOf<DartThrow>()

    private val _displayState = MutableStateFlow(GameDisplayState())
    override val displayState: StateFlow<GameDisplayState> = _displayState.asStateFlow()

    private val _entryMethod = MutableStateFlow<EntryMethod>(EntryMethod.BoardButtons)
    override val activeEntryMethod: StateFlow<EntryMethod> = _entryMethod.asStateFlow()

    override val supportedEntryMethods = listOf(
        EntryMethod.BoardButtons,
        EntryMethod.ScoreInput,
        EntryMethod.Voice,
        EntryMethod.Camera
    )

    override fun addDart(dart: DartThrow) {
        if (currentDarts.size >= 3 || _displayState.value.isFinished) return
        currentDarts.add(dart)

        val state = _displayState.value.turn
        val newTotal = currentDarts.sumOf { it.score() }
        val remaining = (state.remaining ?: 0) - newTotal

        val isCheckout = remaining == 0 && (!engine.doubleOut || dart.isDouble())

        val isBust = remaining < 0 || (remaining == 1 && engine.doubleOut)

        if (currentDarts.size == 3 || isCheckout || isBust) {
            commitTurn()
            return
        }

        refresh()
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun undoLastDart() {
        if (currentDarts.isNotEmpty()) currentDarts.removeLast()
        else {
            val (newState, lastTurn) = engine.undoTurn()
            for (dart in lastTurn.darts.dropLast(1)) {
                currentDarts.add(dart)
            }
        }
        refresh()
    }

    override fun commitTurn() {
        if (currentDarts.isEmpty() || _displayState.value.isFinished) return

        val turnScore = currentDarts.sumOf { it.score() }
        val currentPlayerScore = engine.getState().playerStates[engine.getState().currPlayer].score
        val remaining = currentPlayerScore - turnScore
        val isBust = remaining < 0 || (remaining == 1 && engine.doubleOut)

        val padded = currentDarts.toMutableList()
        while (padded.size < 3) padded.add(DartThrow(0, Multiplier.SINGLE))
        engine.submitTurn(Turn(padded))

        val isWin = engine.getState().isFinished

        currentDarts.clear()
        refresh()

        when {
            isWin -> soundManager.playGameShot()
            isBust -> soundManager.playScore(0)
            else   -> soundManager.playScore(turnScore)
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }

    override fun setEntryMethod(method: EntryMethod) {
        _entryMethod.value = method
    }


    private fun refresh() {
        val engineState   = engine.getState()
        val currentPlayer = engineState.playerStates.getOrNull(engineState.currPlayer)
        val currentScore  = currentPlayer?.score ?: 0
        val turnTotal     = currentDarts.sumOf { it.score() }
        val rawRemaining  = currentScore - turnTotal
        val isBust        = rawRemaining < 0 || (rawRemaining == 1 && engine.doubleOut)
        val remaining     = if (isBust) currentScore else rawRemaining

        val winner = if (engineState.isFinished)
            engineState.playerStates.maxByOrNull { it.legsWon }?.player?.username
        else null

        val legsText = if (engineState.maxLegs == 1) "1 LEG"
        else "BEST OF ${engineState.maxLegs} LEGS"

        _displayState.value = GameDisplayState(
            gameTitle  = legsText,
            players    = engineState.playerStates.mapIndexed { idx, ps ->
                mapPlayer(ps, idx == engineState.currPlayer, engine.target)
            },
            turn = TurnDisplayState(
                slots = List(3) { i -> DartSlotState(currentDarts.getOrNull(i), i) },
                turnScore = if (isBust) 0 else turnTotal,
                remaining = remaining,
                isBust = isBust,
                currentPlayerName = currentPlayer?.player?.username ?: "",
                dartsEnteredCount = currentDarts.size
            ),
            isFinished = engineState.isFinished,
            winner     = winner
        )
    }

    private fun mapPlayer(ps: PlayerStateX01, isCurrent: Boolean, target: Int): PlayerDisplayState {
        val totalScored = target - ps.score
        val avg = if (ps.dartsThrown > 0)
            "%.2f".format((totalScored.toFloat() / ps.dartsThrown) * 3)
        else "0.00"

        val lastScoreText = if (ps.lastScore > 0) ps.lastScore.toString() else "-"

        return PlayerDisplayState(
            id           = ps.player.idPlayer.toString(),
            name         = ps.player.username,
            primaryScore = ps.score.toString(),
            legsWon      = ps.legsWon,
            stats = listOf(
                StatRow("3-dart avg.", avg),
                StatRow("Last score",  lastScoreText),
                StatRow("Darts thrown", ps.dartsThrown.toString())
            ),
            isCurrent = isCurrent
        )
    }
}