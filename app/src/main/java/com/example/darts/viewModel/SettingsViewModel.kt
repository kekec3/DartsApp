package com.example.darts.viewModel

import androidx.lifecycle.ViewModel
import com.example.darts.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _gameType = MutableStateFlow(settingsRepository.getGameType())
    val gameType = _gameType.asStateFlow()

    private val _startingScore = MutableStateFlow(settingsRepository.getStartingScore())
    val startingScore = _startingScore.asStateFlow()

    private val _legs = MutableStateFlow(settingsRepository.getLegs())
    val legs = _legs.asStateFlow()

    private val _doubleOut = MutableStateFlow(settingsRepository.isDoubleOut())
    val doubleOut = _doubleOut.asStateFlow()

    private val _startingPlayerDefault = MutableStateFlow(settingsRepository.getStartingPlayerDefault())
    val startingPlayerDefault = _startingPlayerDefault.asStateFlow()

    private val _soundEffects = MutableStateFlow(settingsRepository.isSoundEffects())
    val soundEffects = _soundEffects.asStateFlow()

    fun setGameType(type: String) {
        settingsRepository.setGameType(type)
        _gameType.value = type
    }

    fun setStartingScore(score: String) {
        settingsRepository.setStartingScore(score)
        _startingScore.value = score
    }

    fun setLegs(legsCount: Int) {
        settingsRepository.setLegs(legsCount)
        _legs.value = legsCount
    }

    fun setDoubleOut(enabled: Boolean) {
        settingsRepository.setDoubleOut(enabled)
        _doubleOut.value = enabled
    }

    fun setStartingPlayerDefault(value: String) {
        settingsRepository.setStartingPlayerDefault(value)
        _startingPlayerDefault.value = value
    }

    fun setSoundEffects(enabled: Boolean) {
        settingsRepository.setSoundEffects(enabled)
        _soundEffects.value = enabled
    }
}