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

    // ---------------- LOBBY LOGIC ----------------

    private val _currentBattleId = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val games: StateFlow<List<Game>> = _currentBattleId
        .filterNotNull()
        .flatMapLatest { id ->
            gameRepository.getGamesByBattle(id)
                ?: flowOf(emptyList())
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

    // ---------------- SETTINGS ----------------

    private val _gameSettings = MutableStateFlow(GameConfig())
    val gameSettings = _gameSettings.asStateFlow()

    fun updateSettings(config: GameConfig) {
        _gameSettings.value = config
    }

    // ---------------- CREATION ----------------

    private val _isCreatingGame = MutableStateFlow(false)
    val isCreatingGame = _isCreatingGame.asStateFlow()

    @SuppressLint("MissingPermission")
    fun saveAndStartGame(
        battleId: Int,
        onComplete: (Int) -> Unit
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

                // GET LOCATION FIRST
                if (_gameSettings.value.trackLocation) {

                    coords = getDeviceLocation()

                    Log.d("LOCATION", "coords = $coords")
                }

                // CREATE GAME
                val newGame = Game(
                    idBattle = battleId,
                    date = dateString,
                    location = coords,
                    duration = 0L,
                    type = _gameSettings.value.type
                )

                val newGameId =
                    gameRepository.createNewGame(newGame).toInt()

                // NAVIGATE
                onComplete(newGameId)

            } catch (e: Exception) {

                Log.e("LOCATION", "saveAndStartGame failed", e)

            } finally {

                _isCreatingGame.value = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getDeviceLocation(): String {

        try {

            // 1. Try cached location first
            val lastLocation =
                fusedLocationClient.lastLocation.await()

            if (lastLocation != null) {

                Log.d(
                    "LOCATION",
                    "Using last location"
                )

                return "${lastLocation.latitude},${lastLocation.longitude}"
            }

            Log.d(
                "LOCATION",
                "Last location null, requesting current location"
            )

            // 2. Fallback to active location request
            val currentLocation = withTimeoutOrNull(10000) {

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    com.google.android.gms.tasks.CancellationTokenSource().token
                ).await()
            }

            if (currentLocation != null) {

                Log.d(
                    "LOCATION",
                    "Using current location"
                )

                return "${currentLocation.latitude},${currentLocation.longitude}"
            }

            Log.d(
                "LOCATION",
                "Current location is null"
            )

        } catch (e: Exception) {

            Log.e(
                "LOCATION",
                "getDeviceLocation failed",
                e
            )
        }

        return ""
    }
}

/**
 * Represents the configuration for a game session.
 */
data class GameConfig(
    val type: String = "501",
    val legs: Int = 5,
    val trackLocation: Boolean = true,
    val checkoutRule: String = "Double Out",
    val showSuggestions: Boolean = true,
    val showAnimations: Boolean = true
)