package com.example.darts.ui.viewModels

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.db.entities.Battle
import com.example.darts.db.entities.Player
import com.example.darts.db.repositories.BattleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BattleViewModel @Inject constructor(
    private val repository: BattleRepository
) : ViewModel() {

    private val _battleName = MutableStateFlow("")
    val battleName = _battleName.asStateFlow()
    private val _existingBattle = MutableStateFlow<Battle?>(null)
    val existingBattle = _existingBattle.asStateFlow()

    val allBattles: StateFlow<List<Battle>> = repository.getAllBattles().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedPlayerIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedPlayerIds = _selectedPlayerIds.asStateFlow()

    // Correct: StateFlow that observes the database Flow
    val availablePlayers: StateFlow<List<Player>> = repository.getAllPlayers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onNameChange(name: String) {
        _battleName.value = name
    }

    fun addNewPlayer(username: String, avatar: String) {
        viewModelScope.launch {
            // 1. Create the object (idPlayer is 0, but Room will replace it)
            val playerToInsert = Player(username = username, avatar = avatar)

            // 2. Insert and get the NEW ID (Cast the Long from Room to your Int)
            val newPlayerId = repository.insertPlayer(playerToInsert)

            val newPlayerIntId = newPlayerId.toInt()

            // 3. Toggle selection using the real ID from the database
            togglePlayer(newPlayerIntId)
        }
    }

    fun togglePlayer(playerId: Int) {
        _selectedPlayerIds.update { current ->
            if (current.contains(playerId)) current - playerId else current + playerId
        }
    }

    // Inside BattleViewModel.kt

    fun saveBattle(
        onSuccess: (Int) -> Unit,
        onDuplicateFound: (Battle) -> Unit
    ) {
        viewModelScope.launch {
            val currentSelection = _selectedPlayerIds.value.sorted()
            var existingBattle: Battle? = null

            val allBattlesList = repository.getAllBattles().first()

            // Loop through battles to find a match
            for (b in allBattlesList) {
                // Use .first() to get the list out of the Flow immediately
                val players = repository.getPlayersOfBattle(b.idBattle)
                    .first()
                    .map { it.idPlayer }
                    .sorted()

                if (players == currentSelection) {
                    existingBattle = b
                    break
                }
            }

            if (existingBattle != null) {
                onDuplicateFound(existingBattle)
            } else {
                repository.createBattleWithPlayers(
                    _battleName.value,
                    _selectedPlayerIds.value.toList()
                )
            }
        }
    }
    fun createNewBattle(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.createBattleWithPlayers(
                _battleName.value,
                _selectedPlayerIds.value.toList()
            )
            onSuccess()
        }
    }
}