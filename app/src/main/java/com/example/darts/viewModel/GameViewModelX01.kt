package com.example.darts.viewModel

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.db.entities.Moment
import com.example.darts.db.entities.MomentType
import com.example.darts.db.entities.Player
import com.example.darts.db.entities.PlayerLegStats
import com.example.darts.db.repositories.BattleRepository
import com.example.darts.db.repositories.GameRepository
import com.example.darts.db.repositories.MomentRepository
import com.example.darts.db.repositories.StatRepository
import com.example.darts.engine.DartThrow
import com.example.darts.engine.GameEngineX01
import com.example.darts.engine.Multiplier
import com.example.darts.engine.Turn
import com.example.darts.ui.screens.score_entry.EntryMethod
import com.example.darts.utils.SoundManager
import com.example.darts.viewModel.states.DartSlotState
import com.example.darts.viewModel.states.GameDisplayState
import com.example.darts.viewModel.states.GameState
import com.example.darts.viewModel.states.PlayerDisplayState
import com.example.darts.viewModel.states.PlayerStateX01
import com.example.darts.viewModel.states.StatRow
import com.example.darts.viewModel.states.TurnDisplayState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModelX01 @Inject constructor(
    private val gameRepository: GameRepository,
    private val battleRepository: BattleRepository,
    private val momentRepository: MomentRepository,
    private val soundManager: SoundManager,
    private val statRepository: StatRepository
) : ViewModel(), BaseGameViewModel {

    private val _navigationEvents = MutableSharedFlow<GameNavigationEvent>()
    override val navigationEvents = _navigationEvents.asSharedFlow()

    private val _turnHistory = MutableStateFlow<List<TurnSummary>>(emptyList())
    override val turnHistory: StateFlow<List<TurnSummary>> = _turnHistory.asStateFlow()

    override fun consumeNavigationEvent() {

    }

    private var isInitialized = false

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

    override fun loadGame(gameId: Int, maxLegs: Int, config: GameConfig) {
        if (isInitialized) return
        isInitialized = true

        val cfg = config as XO1Config
        this.gameId = gameId

        viewModelScope.launch {
            val game = gameRepository.getGameById(gameId) ?: return@launch
            val players = battleRepository.getPlayersOfBattle(game.idBattle)
            val playerStates = players.map { PlayerStateX01(it) }
            startGame(
                players = playerStates,
                target = cfg.target,
                doubleOut = cfg.doubleOut,
                masterIn = cfg.masterIn,
                maxLegs = maxLegs
            )
        }

        _turnHistory.value = emptyList()
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

        // Bug fix: derive remaining directly from the engine's authoritative score,
        // not from the display-state snapshot. The snapshot already has previous darts
        // subtracted, so using it as a base would double-count them on dart 2 and 3.
        val engineState = engine.getState()
        val currentScore = engineState.playerStates[engineState.currPlayer].score
        val newTotal = currentDarts.sumOf { it.score() }
        val remaining = currentScore - newTotal

        val isCheckout = remaining == 0 && (!engine.doubleOut || dart.isDouble())
        val isBust = remaining < 0 || (remaining == 1 && engine.doubleOut)

        if (currentDarts.size == 3 || isCheckout || isBust) {
            commitTurn()
            return
        }

        refresh()
    }

    override fun undoLastDart() {
        if (currentDarts.isNotEmpty()) {
            currentDarts.removeAt(currentDarts.size - 1)
        } else {
            val (newState, lastTurn) = engine.undoTurn()
            for (dart in lastTurn.darts.dropLast(1)) {
                currentDarts.add(dart)
            }
            _turnHistory.value = _turnHistory.value.dropLast(1)
        }
        refresh()
    }

    override fun commitTurn() {
        if (currentDarts.isEmpty() || _displayState.value.isFinished) return

        val preTurnState = engine.getState()
        val turnScore    = currentDarts.sumOf { it.score() }
        val remaining    = preTurnState.playerStates[preTurnState.currPlayer].score - turnScore
        val isBust       = remaining < 0 || (remaining == 1 && engine.doubleOut)

        // Bug fix: do NOT pad to 3 darts. Padding caused the engine to count every
        // turn as exactly 3 darts thrown (corrupting averages), and broke undo —
        // dropLast(1) on a padded list removed the padding instead of the real
        // checkout dart. Submit only the darts that were actually thrown.
        val newState = engine.submitTurn(Turn(currentDarts.toList()))

        _turnHistory.value = engine.turnHistory.mapIndexed { index, t ->
            TurnSummary(
                turnNumber    = index + 1,
                playerName    = preTurnState.playerStates[index % preTurnState.playerStates.size].player.username,
                dartDisplays  = t.darts.map { it.displayString() },
                turnScore     = t.darts.sumOf { it.score() },
                remainingAfter = remaining      // null if Cricket
            )
        }

        currentDarts.clear()
        refresh()

        // ── Persist stats whenever a leg ends ────────────────────────────
        if (newState.legJustCompleted) {
            persistLegStats(newState)
        }

        // ── Sounds ───────────────────────────────────────────────────────
        when {
            newState.isFinished -> soundManager.playGameShot()
            isBust              -> soundManager.playScore(0)
            else                -> soundManager.playScore(turnScore)
        }
    }

    private fun persistLegStats(newState: GameState<PlayerStateX01>) {
        viewModelScope.launch {
            val rows = newState.completedLegStats.mapIndexed { index, ps ->
                val avg = if (ps.legDartsThrown > 0)
                    (ps.legTotalScored.toFloat() / ps.legDartsThrown) * 3f
                else 0f
                PlayerLegStats(
                    gameId = gameId,
                    playerId = ps.player.idPlayer,
                    legNumber = newState.completedLegNumber,
                    won = (index == newState.completedLegWinnerIndex),
                    dartsThrown = ps.legDartsThrown,
                    totalScored = ps.legTotalScored,
                    average = avg,
                    checkoutAttempts = ps.legCheckoutAttempts,
                    checkoutsHit = ps.legCheckoutsHit,
                    highestCheckout = ps.legHighestCheckout,
                    scores180 = ps.leg180s,
                    scores140Plus = ps.leg140Plus,
                    scores100Plus = ps.leg100Plus,
                )
            }
            val savedRows = statRepository.saveLegStats(rows)

            val matchWinnerId = if (newState.isFinished)
                newState.playerStates.maxByOrNull { it.legsWon }?.player?.idPlayer
            else null

            statRepository.updateCareerStats(
                legStats      = savedRows,
                isMatchEnd    = newState.isFinished,
                matchWinnerId = matchWinnerId,
            )

            if (newState.isFinished) {
                gameRepository.markGameFinished(gameId)

                _navigationEvents.emit(GameNavigationEvent.MatchSummary(gameId))
            } else {
                val showLegSummary = true
                if (showLegSummary) {
                    _navigationEvents.emit(
                        GameNavigationEvent.LegSummary(
                            gameId = gameId,
                            legNumber = newState.completedLegNumber
                        )
                    )
                }
            }

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