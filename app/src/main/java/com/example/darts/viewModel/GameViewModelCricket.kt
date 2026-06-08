package com.example.darts.viewModel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.db.entities.Moment
import com.example.darts.db.entities.MomentType
import com.example.darts.db.repositories.BattleRepository
import com.example.darts.db.repositories.GameRepository
import com.example.darts.db.repositories.MomentRepository
import com.example.darts.engine.DartThrow
import com.example.darts.engine.GameEngineCricket
import com.example.darts.engine.Multiplier
import com.example.darts.engine.Turn
import com.example.darts.ui.screens.score_entry.EntryMethod
import com.example.darts.viewModel.states.DartSlotState
import com.example.darts.viewModel.states.GameDisplayState
import com.example.darts.viewModel.states.PlayerDisplayState
import com.example.darts.viewModel.states.PlayerStateCricket
import com.example.darts.viewModel.states.StatRow
import com.example.darts.viewModel.states.TurnDisplayState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// State object specific to Cricket for UI rendering (marks)
data class CricketUiState(
    val playerStates: List<PlayerStateCricket> = emptyList(),
    val currentPlayerIndex: Int = 0
)

@HiltViewModel
class GameViewModelCricket @Inject constructor(
    private val gameRepository: GameRepository,
    private val battleRepository: BattleRepository,
    private val momentRepository: MomentRepository,
) : ViewModel(), BaseGameViewModel {

    private lateinit var engine: GameEngineCricket
    private var gameId: Int = -1

    fun startGame(players: List<PlayerStateCricket>, cutthroat: Boolean = false, maxLegs: Int = 3) {
        engine = GameEngineCricket(players, cutTroath = cutthroat, maxLegs = maxLegs)
        refresh()
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

    override fun loadGame(gameId: Int, maxLegs: Int, config: GameConfig
    ) {
        val cfg = config as CricketConfig
        this.gameId = gameId

        viewModelScope.launch {
            val game = gameRepository.getGameById(gameId) ?: return@launch
            val players = battleRepository.getPlayersOfBattle(game.idBattle)
            val playerStates = players.map { PlayerStateCricket(it) }
            startGame(
                players = playerStates,
                cutthroat = cfg.cutthroat,
                maxLegs = maxLegs
            )
        }
    }

    private val currentDarts = mutableListOf<DartThrow>()

    private val _displayState = MutableStateFlow(GameDisplayState())
    override val displayState: StateFlow<GameDisplayState> = _displayState.asStateFlow()

    // Specifically expose cricket marks to the UI
    private val _cricketUiState = MutableStateFlow(CricketUiState())
    val cricketUiState: StateFlow<CricketUiState> = _cricketUiState.asStateFlow()

    private val _entryMethod = MutableStateFlow<EntryMethod>(EntryMethod.Cricket)
    override val activeEntryMethod: StateFlow<EntryMethod> = _entryMethod.asStateFlow()

    override val supportedEntryMethods = listOf(
        EntryMethod.Cricket,
        EntryMethod.Voice
    )

    override fun addDart(dart: DartThrow) {
        Log.d("Cricket Viewmodel", "Dart Add")
        if (currentDarts.size >= 3 || _displayState.value.isFinished) return
        currentDarts.add(dart)
        if (currentDarts.size == 3) {
            commitTurn()
            return
        }
        refresh() // Ensure dots and slots update
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun undoLastDart() {
        if (currentDarts.isNotEmpty()) currentDarts.removeAt(currentDarts.size - 1)
        else {
            val (newState, turn) = engine.undoTurn()
            for (dart in turn.darts.dropLast(1)) {
                currentDarts.add(dart)
            }
        }
        refresh()
    }

    override fun commitTurn() {
        Log.d("Commit Turn", "Enter")
        val padded = currentDarts.toMutableList()
        while (padded.size < 3) padded.add(DartThrow(0, Multiplier.SINGLE))
        engine.submitTurn(Turn(padded))
        currentDarts.clear()
        refresh()
    }

    override fun setEntryMethod(method: EntryMethod) {
        _entryMethod.value = method
    }

    private fun refresh() {
        val engineState = engine.getState()
        val currentPlayer = engineState.playerStates.getOrNull(engineState.currPlayer)
        val turnTotal = currentDarts.sumOf { it.score() }

        val winner = if (engineState.isFinished)
            engineState.playerStates.maxByOrNull { it.legsWon }?.player?.username
        else null

        val legsText = if (engineState.maxLegs == 1) "1 LEG"
        else "BEST OF ${engineState.maxLegs} LEGS"

        _displayState.value = GameDisplayState(
            gameTitle = legsText,
            players = engineState.playerStates.mapIndexed { idx, ps ->
                mapPlayer(ps, idx == engineState.currPlayer)
            },
            turn = TurnDisplayState(
                slots = List(3) { i -> DartSlotState(currentDarts.getOrNull(i), i) },
                turnScore = turnTotal,
                remaining = null, // No remaining in cricket
                isBust = false, // No bust in cricket
                currentPlayerName = currentPlayer?.player?.username ?: "",
                dartsEnteredCount = currentDarts.size
            ),
            isFinished = engineState.isFinished,
            winner = winner
        )

        _cricketUiState.value = CricketUiState(
            playerStates = engineState.playerStates,
            currentPlayerIndex = engineState.currPlayer
        )
    }

    private fun mapPlayer(ps: PlayerStateCricket, isCurrent: Boolean): PlayerDisplayState {
        val totalMarks = ps.numbers.values.sumOf { it.marks }
        val mpr = if (ps.dartsThrown > 0)
            "%.2f".format((totalMarks.toFloat() / ps.dartsThrown) * 3)
        else "0.00"

        return PlayerDisplayState(
            id = ps.player.idPlayer.toString(),
            name = ps.player.username,
            primaryScore = ps.score.toString(), // Main score populated here
            legsWon = ps.legsWon,
            stats = listOf(
                StatRow("MPR (Marks/Round)", mpr),
                StatRow("Darts thrown", ps.dartsThrown.toString())
            ),
            isCurrent = isCurrent
        )
    }
}