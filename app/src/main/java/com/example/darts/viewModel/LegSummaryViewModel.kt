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

data class LegPlayerStat(
    val playerId: Int,
    val playerName: String,
    val won: Boolean,
    val stats: PlayerLegStats
)

data class LegSummaryUiState(
    val players: List<LegPlayerStat> = emptyList(),
    val gameMode: GameMode = GameMode.X01,
    val winnerName: String = ""
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
            if (rows.isEmpty()) return@launch

            val game = gameRepository.getGameById(gameId)?.type ?: "x01"
            val mode = if (game.lowercase() == "cricket") GameMode.CRICKET else GameMode.X01

            val playerStatsList = rows.mapNotNull { stat ->
                val player = playerRepository.getPlayerById(stat.playerId)
                player?.let {
                    LegPlayerStat(
                        playerId = it.idPlayer,
                        playerName = it.username,
                        won = stat.won,
                        stats = stat
                    )
                }
            }.sortedWith(
                compareByDescending<LegPlayerStat> { it.won }
                    .thenByDescending { it.stats.average }
            )

            val winner = playerStatsList.firstOrNull { it.won }?.playerName ?: ""

            _uiState.value = LegSummaryUiState(
                players = playerStatsList,
                gameMode = mode,
                winnerName = winner
            )
            Log.d("LEG_VM", "Found ${rows.size} rows, mode=$mode")
        }
    }
}