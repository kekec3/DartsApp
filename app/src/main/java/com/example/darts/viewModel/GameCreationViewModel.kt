package com.example.darts.viewModel

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.db.entities.Game
import com.example.darts.db.entities.Player
import com.example.darts.db.repositories.GameRepository
import com.example.darts.db.repositories.BattleRepository
import com.example.darts.repository.SettingsRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GameCreationViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val battleRepository: BattleRepository,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _currentBattleId = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val games: StateFlow<List<Game>> = _currentBattleId
        .filterNotNull()
        .flatMapLatest { id ->
            gameRepository.getGamesByBattle(id) ?: flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Dynamic player flow for the current active battle context
    @OptIn(ExperimentalCoroutinesApi::class)
    val battlePlayers: StateFlow<List<Player>> = _currentBattleId
        .filterNotNull()
        .mapLatest { id -> // Changed from flatMapLatest to mapLatest
            battleRepository.getPlayersOfBattle(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    fun loadBattle(battleId: Int) {
        if (_currentBattleId.value != battleId) {
            _currentBattleId.value = battleId
        }
    }

    private val _gameSettings = MutableStateFlow(
        GameSettings(
            type = settingsRepository.getGameType(),
            startingScore = settingsRepository.getStartingScore(),
            legs = settingsRepository.getLegs(),
            doubleOut = settingsRepository.isDoubleOut(),
            masterIn = settingsRepository.isMasterIn(),
            cutThroat = settingsRepository.isCutThroat(),
            showSuggestions = settingsRepository.isShowSuggestions(),
            trackLocation = settingsRepository.isTrackLocation(),
            startingPlayerId = when (settingsRepository.getStartingPlayerDefault().lowercase()) {
                "random" -> -1
                else -> -2
            }
        )
    )
    val gameSettings = _gameSettings.asStateFlow()

    fun updateSettings(settings: GameSettings) {
        _gameSettings.value = settings
    }

    fun resetToDefaults() {
        _gameSettings.value = GameSettings(
            type = settingsRepository.getGameType(),
            startingScore = settingsRepository.getStartingScore(),
            legs = settingsRepository.getLegs(),
            doubleOut = settingsRepository.isDoubleOut(),
            masterIn = settingsRepository.isMasterIn(),
            cutThroat = settingsRepository.isCutThroat(),
            showSuggestions = settingsRepository.isShowSuggestions(),
            trackLocation = settingsRepository.isTrackLocation(),
            startingPlayerId = when (settingsRepository.getStartingPlayerDefault().lowercase()) {
                "random" -> -1
                else -> -2
            }
        )
    }

    private val _isCreatingGame = MutableStateFlow(false)
    val isCreatingGame = _isCreatingGame.asStateFlow()

    @SuppressLint("MissingPermission")
    fun saveAndStartGame(
        battleId: Int,
        onComplete: (Int) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (_isCreatingGame.value) return

        _isCreatingGame.value = true

        viewModelScope.launch {
            try {
                val dateString = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())

                var coords = ""

                if (_gameSettings.value.trackLocation) {
                    coords = getDeviceLocation()
                    if (coords.isEmpty() || coords == "0.0,0.0") {
                        Log.e("LOCATION", "Tracking enabled but coordinates couldn't be fetched.")
                        _isCreatingGame.value = false
                        onError("Failed to acquire exact GPS coordinates. Please try again.")
                        return@launch
                    }
                }

                val newGame = Game(
                    idBattle = battleId,
                    date = dateString,
                    location = coords,
                    duration = 0L,
                    type = _gameSettings.value.type
                )

                val newGameId = gameRepository.createNewGame(newGame).toInt()
                onComplete(newGameId)

            } catch (e: Exception) {
                Log.e("LOCATION", "saveAndStartGame failed", e)
                onError(e.message ?: "An unexpected error occurred.")
            } finally {
                _isCreatingGame.value = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getDeviceLocation(): String {
        try {
            val currentLocation = withTimeoutOrNull(12000) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    com.google.android.gms.tasks.CancellationTokenSource().token
                ).await()
            }

            if (currentLocation != null) {
                return "${currentLocation.latitude},${currentLocation.longitude}"
            }

            val lastLocation = fusedLocationClient.lastLocation.await()
            if (lastLocation != null) {
                return "${lastLocation.latitude},${lastLocation.longitude}"
            }
        } catch (e: Exception) {
            Log.e("LOCATION", "getDeviceLocation execution failed", e)
        }
        return ""
    }
}

/**
 * Flat UI-level settings collected on the Game Settings screen[cite: 4].
 */
data class GameSettings(
    val type: String = "x01",
    val startingScore: String = "501",
    val legs: Int = 3,
    val doubleOut: Boolean = false,
    val masterIn: Boolean = false,
    val cutThroat: Boolean = false,
    val showSuggestions: Boolean = true,
    val trackLocation: Boolean = false,
    val startingPlayerId: Int = -1 // -1 = Random, -2 = Default Order, >0 = Player ID
)