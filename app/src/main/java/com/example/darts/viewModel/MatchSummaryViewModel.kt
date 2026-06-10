package com.example.darts.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.db.repositories.PlayerRepository
import com.example.darts.db.repositories.StatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MatchPlayerStats(
    val playerId: Int,
    val playerName: String,
    val average: Float,          // 3-dart avg for X01, MPR for Cricket
    val checkoutPercent: Float,  // X01 only
    val highestCheckout: Int,    // X01 only
    val scores180: Int,          // X01 only
    val legsWon: Int,
)

data class MatchSummaryUiState(
    val players: List<MatchPlayerStats> = emptyList(),
    val gameMode: GameMode = GameMode.X01,
)

@HiltViewModel
class MatchSummaryViewModel @Inject constructor(
    private val statRepository: StatRepository,
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchSummaryUiState())
    val uiState = _uiState.asStateFlow()

    fun load(gameId: Int) {
        viewModelScope.launch {
            val rows = statRepository.getMatchStats(gameId)
            val grouped = rows.groupBy { it.playerId }

            // Detect mode from data
            val mode = if (rows.all { it.checkoutAttempts == 0 && it.highestCheckout == 0 })
                GameMode.CRICKET else GameMode.X01

            val result = grouped.map { (playerId, stats) ->
                val player = playerRepository.getPlayerById(playerId)
                val scored = stats.sumOf { it.totalScored }
                val darts = stats.sumOf { it.dartsThrown }
                val attempts = stats.sumOf { it.checkoutAttempts }
                val hits = stats.sumOf { it.checkoutsHit }

                MatchPlayerStats(
                    playerId = playerId,
                    playerName = player?.username ?: "",
                    average = if (darts > 0) scored.toFloat() / darts * 3 else 0f,
                    checkoutPercent = if (attempts > 0) hits * 100f / attempts else 0f,
                    highestCheckout = stats.maxOf { it.highestCheckout },
                    scores180 = stats.sumOf { it.scores180 },
                    legsWon = stats.count { it.won },
                )
            }

            _uiState.value = MatchSummaryUiState(players = result, gameMode = mode)
        }
    }
}