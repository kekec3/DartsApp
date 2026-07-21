package com.example.darts.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.db.entities.Player
import com.example.darts.db.entities.PlayerCareerStats
import com.example.darts.db.repositories.PlayerRepository
import com.example.darts.db.repositories.StatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerStatsViewModel @Inject constructor(
    private val statRepository: StatRepository,
    private val playerRepository: PlayerRepository
) : ViewModel() {

    val allPlayers: StateFlow<List<Player>> = playerRepository.getAllPlayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPlayer = MutableStateFlow<Player?>(null)
    val selectedPlayer = _selectedPlayer.asStateFlow()

    private val _careerStats = MutableStateFlow<PlayerCareerStats?>(null)
    val careerStats = _careerStats.asStateFlow()

    /**
     * Per-leg 3-dart averages in chronological order (oldest → newest).
     * Drives the trend line chart. Updated live as new legs are recorded.
     */
    private val _legTrend = MutableStateFlow<List<Float>>(emptyList())
    val legTrend = _legTrend.asStateFlow()

    private var statsJob: Job? = null
    private var trendJob: Job? = null

    init {
        // Automatically default to the first player when opening stats without a specific ID
        viewModelScope.launch {
            allPlayers.collect { players ->
                if (_selectedPlayer.value == null && players.isNotEmpty()) {
                    selectPlayer(players.first())
                }
            }
        }
    }

    fun load(playerId: Int) {
        viewModelScope.launch {
            allPlayers.collect { players ->
                val player = players.find { it.idPlayer == playerId }
                if (player != null) {
                    selectPlayer(player)
                } else if (playerId != -1) {
                    loadStatsForId(playerId)
                }
            }
        }
    }

    fun selectPlayer(player: Player) {
        _selectedPlayer.value = player
        loadStatsForId(player.idPlayer)
    }

    private fun loadStatsForId(playerId: Int) {
        statsJob?.cancel()
        trendJob?.cancel()

        statsJob = viewModelScope.launch {
            statRepository
                .observeCareerStats(playerId)
                .collect { _careerStats.value = it }
        }

        trendJob = viewModelScope.launch {
            statRepository
                .observeLegStatsForPlayer(playerId)
                .collect { legs -> _legTrend.value = legs.map { it.average } }
        }
    }
}