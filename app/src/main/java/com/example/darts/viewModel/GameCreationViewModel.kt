package com.example.darts.viewModel

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.db.entities.Game
import com.example.darts.db.repositories.GameRepository
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
    private val fusedLocationClient: FusedLocationProviderClient
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

    fun loadBattle(battleId: Int) {
        if (_currentBattleId.value != battleId) {
            _currentBattleId.value = battleId
        }
    }

    private val _gameSettings = MutableStateFlow(GameSettings())
    val gameSettings = _gameSettings.asStateFlow()

    fun updateSettings(settings: GameSettings) {
        _gameSettings.value = settings
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
                val dateString = SimpleDateFormat(
                    "dd.MM.yyyy HH:mm",
                    Locale.getDefault()
                ).format(Date())

                var coords = ""

                if (_gameSettings.value.trackLocation) {
                    coords = getDeviceLocation()

                    if (coords.isEmpty() || coords == "0.0,0.0") {
                        Log.e("LOCATION", "Tracking enabled but coordinates couldn't be fetched.")
                        _isCreatingGame.value = false
                        onError("Failed to acquire exact GPS coordinates. Please try again.")
                        return@launch
                    }
                    Log.d("LOCATION", "Acquired coords: $coords")
                } else {
                    Log.d("LOCATION", "Tracking disabled. Creating game without location.")
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
            Log.d("LOCATION", "Requesting high-accuracy fresh current location")
            val currentLocation = withTimeoutOrNull(12000) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    com.google.android.gms.tasks.CancellationTokenSource().token
                ).await()
            }

            if (currentLocation != null) {
                return "${currentLocation.latitude},${currentLocation.longitude}"
            }

            Log.d("LOCATION", "Fresh location timed out, fallback to last known location")
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
 * Flat UI-level settings collected on the Game Settings screen.
 * Converted to the appropriate [GameConfig] subtype when navigating
 * to the actual game screen.
 */
data class GameSettings(
    val type: String = "x01",           // "x01" | "cricket"
    val startingScore: String = "501",  // x01 only: "301" | "501" | "701"
    val legs: Int = 3,
    // x01 modifiers
    val doubleOut: Boolean = false,
    val masterIn: Boolean = false,
    // cricket modifiers
    val cutThroat: Boolean = false,
    // general
    val showSuggestions: Boolean = true,
    val trackLocation: Boolean = false,
    val showAnimations: Boolean = true
)