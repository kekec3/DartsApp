package com.example.darts.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.darts.db.entities.Game
import com.example.darts.db.repositories.GameRepository
import com.example.darts.ui.navigation.MapRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: GameRepository
) : ViewModel() {

    private val mapRoute = savedStateHandle.toRoute<MapRoute>()

    private val _games = MutableStateFlow<List<Game>>(emptyList())
    val games: StateFlow<List<Game>> = _games.asStateFlow()

    init {
        loadGames()
    }

    private fun loadGames() {
        viewModelScope.launch {
            val battleId = mapRoute.battleId

            // 1. Dobijamo Flow iz repozitorijuma
            val gamesFlow = if (battleId == null) {
                repository.getAllGames()
            } else {
                repository.getGamesByBattle(battleId)
            }

            // 2. Skupljamo (collect) podatke iz Flow-a i stavljamo ih u _games
            gamesFlow.collect { gamesList ->
                _games.value = gamesList
            }
        }
    }
}