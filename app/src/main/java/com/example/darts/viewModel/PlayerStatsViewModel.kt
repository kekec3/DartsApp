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

    private val _careerStats =
        MutableStateFlow<PlayerCareerStats?>(null)

    val careerStats =
        _careerStats.asStateFlow()

    fun load(playerId: Int) {

        viewModelScope.launch {

            statRepository
                .observeCareerStats(playerId)
                .collect {
                    _careerStats.value = it
                }
        }
    }
}