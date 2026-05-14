package com.example.darts.viewModel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.db.entities.Game
import com.example.darts.db.repositories.GameRepository
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel responsible for managing the "Lobby" state (games in a battle)
 * and the configuration for creating a new game.
 */
@HiltViewModel
class GameCreationViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {

    // --- LOBBY LOGIC ---

    private val _currentBattleId = MutableStateFlow<Int?>(null)

    /**
     * Exposes a reactive list of games for the currently loaded battle.
     * flatMapLatest ensures that if the battleId changes, we switch to the new database flow.
     */
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

    /**
     * Called when entering the GameCreateScreen to set the context for the lobby.
     */
    fun loadBattle(battleId: Int) {
        if (_currentBattleId.value != battleId) {
            _currentBattleId.value = battleId
        }
    }

    // --- SETTINGS LOGIC ---

    private val _gameSettings = MutableStateFlow(GameConfig())
    val gameSettings = _gameSettings.asStateFlow()

    fun updateSettings(config: GameConfig) {
        _gameSettings.value = config
    }

    fun updateGameType(type: String) {
        _gameSettings.value = _gameSettings.value.copy(type = type)
    }

    fun updateLegs(legs: Int) {
        _gameSettings.value = _gameSettings.value.copy(legs = legs)
    }

    // --- CREATION LOGIC ---

    /**
     * Creates a new game entry in the database and triggers navigation via onComplete.
     */
    @SuppressLint("MissingPermission")
    fun saveAndStartGame(battleId: Int, onComplete: (Int) -> Unit) {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val dateString = formatter.format(Date())

        viewModelScope.launch {
            // 1. Create the game record IMMEDIATELY with a placeholder location
            val newGame = Game(
                idBattle = battleId,
                date = dateString,
                location = "0.0,0.0", // Placeholder
                duration = 0L,
                type = _gameSettings.value.type
            )

            val newGameId = gameRepository.createNewGame(newGame)

            // 2. Trigger Navigation immediately so the user can play
            onComplete(newGameId.toInt())

            // 3. Attempt to fetch location in the background and update the record
            try {
                fusedLocationClient.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    com.google.android.gms.tasks.CancellationTokenSource().token
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        val coords = "${location.latitude},${location.longitude}"

                        // Update the existing record in the DB with actual coordinates
                        viewModelScope.launch {
                            gameRepository.updateGameLocation(newGameId.toInt(), coords)
                        }
                    }
                }
            } catch (e: SecurityException) {
                // User denied permission; placeholder "0.0,0.0" stays
            }
        }
    }
}

/**
 * Represents the UI state for the GameSettingsScreen.
 */
data class GameConfig(
    val type: String = "501",
    val legs: Int = 5,
    val checkoutRule: String = "Double Out",
    val showSuggestions: Boolean = true,
    val showAnimations: Boolean = true
)