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

            // Ako nema battleId-ja, učitaj sve. Ako ima, učitaj samo za tu bitku.
            _games.value = (if (battleId == null) {
                repository.getAllGames()
            } else {
                repository.getGamesByBattle(battleId)
            }) as List<Game>
        }
    }
}