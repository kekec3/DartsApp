package com.example.darts.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.db.repositories.BattleRepository
import com.example.darts.db.repositories.GameRepository
import com.example.darts.db.repositories.PlayerRepository
import com.example.darts.db.repositories.StatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BattlePlayerStats(
    val playerId: Int,
    val playerName: String,
    val matchesWon: Int,
    val totalLegsWon: Int,
    val overallAverage: Float
)

data class BattleSummaryUiState(
    val players: List<BattlePlayerStats> = emptyList(),
    val totalMatchesPlayed: Int = 0,
    val winnerName: String = ""
)

@HiltViewModel
class BattleSummaryViewModel @Inject constructor(
    private val battleRepository: BattleRepository,
    private val gameRepository: GameRepository,
    private val statRepository: StatRepository,
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BattleSummaryUiState())
    val uiState = _uiState.asStateFlow()

    fun load(battleId: Int) {
        viewModelScope.launch {
            // Collect the games Flow from repository
            gameRepository.getGamesByBattle(battleId).collect { games ->
                if (games.isEmpty()) {
                    _uiState.value = BattleSummaryUiState()
                    return@collect
                }

                val playerMatchWins = mutableMapOf<Int, Int>()
                val playerLegWins = mutableMapOf<Int, Int>()
                val playerTotalScored = mutableMapOf<Int, Int>()
                val playerTotalDarts = mutableMapOf<Int, Int>()

                var totalMatches = 0

                for (game in games) {
                    val matchStats = statRepository.getMatchStats(game.idGame)
                    if (matchStats.isEmpty()) continue

                    totalMatches++

                    val legsPerPlayer = matchStats.groupBy { it.playerId }
                    var highestLegsInMatch = -1
                    var matchWinnerId: Int? = null

                    legsPerPlayer.forEach { (playerId, statsList) ->
                        val legsWonInMatch = statsList.count { it.won }
                        playerLegWins[playerId] = (playerLegWins[playerId] ?: 0) + legsWonInMatch

                        val scored = statsList.sumOf { it.totalScored }
                        val darts = statsList.sumOf { it.dartsThrown }
                        playerTotalScored[playerId] = (playerTotalScored[playerId] ?: 0) + scored
                        playerTotalDarts[playerId] = (playerTotalDarts[playerId] ?: 0) + darts

                        if (legsWonInMatch > highestLegsInMatch) {
                            highestLegsInMatch = legsWonInMatch
                            matchWinnerId = playerId
                        }
                    }

                    matchWinnerId?.let { winnerId ->
                        playerMatchWins[winnerId] = (playerMatchWins[winnerId] ?: 0) + 1
                    }
                }

                val allPlayerIds = (playerLegWins.keys + playerMatchWins.keys).toSet()

                val rankedPlayers = allPlayerIds.mapNotNull { playerId ->
                    val player = playerRepository.getPlayerById(playerId)
                    val scored = playerTotalScored[playerId] ?: 0
                    val darts = playerTotalDarts[playerId] ?: 0
                    val avg = if (darts > 0) scored.toFloat() / darts * 3 else 0f

                    player?.let {
                        BattlePlayerStats(
                            playerId = it.idPlayer,
                            playerName = it.username,
                            matchesWon = playerMatchWins[playerId] ?: 0,
                            totalLegsWon = playerLegWins[playerId] ?: 0,
                            overallAverage = avg
                        )
                    }
                }.sortedWith(
                    compareByDescending<BattlePlayerStats> { it.matchesWon }
                        .thenByDescending { it.totalLegsWon }
                        .thenByDescending { it.overallAverage }
                )

                val winner = rankedPlayers.firstOrNull()?.playerName ?: ""

                _uiState.value = BattleSummaryUiState(
                    players = rankedPlayers,
                    totalMatchesPlayed = totalMatches,
                    winnerName = winner
                )
            }
        }
    }
}