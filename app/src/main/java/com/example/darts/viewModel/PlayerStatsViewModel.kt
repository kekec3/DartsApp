package com.example.darts.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.db.entities.PlayerCareerStats
import com.example.darts.db.repositories.StatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerStatsViewModel @Inject constructor(
    private val statRepository: StatRepository
) : ViewModel() {

    private val _careerStats = MutableStateFlow<PlayerCareerStats?>(null)
    val careerStats = _careerStats.asStateFlow()

    /**
     * Per-leg 3-dart averages in chronological order (oldest → newest).
     * Drives the trend line chart. Updated live as new legs are recorded.
     */
    private val _legTrend = MutableStateFlow<List<Float>>(emptyList())
    val legTrend = _legTrend.asStateFlow()

    fun load(playerId: Int) {
        viewModelScope.launch {
            statRepository
                .observeCareerStats(playerId)
                .collect { _careerStats.value = it }
        }

        viewModelScope.launch {
            statRepository
                .observeLegStatsForPlayer(playerId)
                .collect { legs -> _legTrend.value = legs.map { it.average } }
        }
    }
}