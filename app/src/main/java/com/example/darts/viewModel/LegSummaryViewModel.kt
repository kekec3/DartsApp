package com.example.darts.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.db.entities.PlayerLegStats
import com.example.darts.db.repositories.GameRepository
import com.example.darts.db.repositories.PlayerRepository
import com.example.darts.db.repositories.StatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GameMode { X01, CRICKET }

data class LegSummaryUiState(
    val player1Name: String = "",
    val player2Name: String = "",
    val player1Stats: PlayerLegStats? = null,
    val player2Stats: PlayerLegStats? = null,
    val gameMode: GameMode = GameMode.X01,
)

@HiltViewModel
class LegSummaryViewModel @Inject constructor(
    private val statRepository: StatRepository,
    private val playerRepository: PlayerRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LegSummaryUiState())
    val uiState = _uiState.asStateFlow()

    fun load(gameId: Int, legNumber: Int) {
        Log.d("LEG_VM", "Loading game=$gameId leg=$legNumber")
        viewModelScope.launch {
            val rows = statRepository.getLegStats(gameId, legNumber)
            if (rows.size < 2) return@launch

            val p1 = rows[0]
            val p2 = rows[1]

            val player1 = playerRepository.getPlayerById(p1.playerId)
            val player2 = playerRepository.getPlayerById(p2.playerId)

            val game = gameRepository.getGameById(gameId)?.type ?: "x01"
            val mode = if (game.lowercase() == "cricket") GameMode.CRICKET else GameMode.X01

            _uiState.value = LegSummaryUiState(
                player1Name = player1?.username ?: "",
                player2Name = player2?.username ?: "",
                player1Stats = p1,
                player2Stats = p2,
                gameMode = mode,
            )
            Log.d("LEG_VM", "Found ${rows.size} rows, mode=$mode")
        }
    }
}