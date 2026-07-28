package com.example.darts.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.db.entities.Game
import com.example.darts.db.repositories.BattleRepository
import com.example.darts.db.repositories.GameRepository
import com.example.darts.repository.SettingsRepository
import com.example.darts.ui.screens.QuickGameMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuickPlayViewModel @Inject constructor(
    private val battleRepository: BattleRepository,
    private val gameRepository: GameRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    fun launchQuickMatch(
        playerCount: Int,
        mode: QuickGameMode,
        onSuccess: (
            gameId: Int,
            isCricket: Boolean,
            legs: Int,
            target: Int,
            doubleOut: Boolean,
            masterIn: Boolean,
            cutThroat: Boolean
        ) -> Unit
    ) {
        viewModelScope.launch {
            val battleId = battleRepository.getQuickPlayBattleId(playerCount)

            // Get match configurations from SettingsRepository
            val legs = settingsRepository.getLegs()
            val targetScore = settingsRepository.getStartingScore().toIntOrNull() ?: 501
            val doubleOut = settingsRepository.isDoubleOut()
            val masterIn = settingsRepository.isMasterIn()
            val cutThroat = settingsRepository.isCutThroat()

            val gameType = if (mode == QuickGameMode.CRICKET) "cricket" else "x01"

            // Construct Game instance matching Game.kt definition
            val newGame = Game(
                idBattle = battleId,
                date = System.currentTimeMillis().toString(),
                location = "",
                duration = 0L,
                type = gameType,
                finished = false,
                history = null
            )

            // Create record using GameRepository
            val gameId = gameRepository.createNewGame(newGame)

            onSuccess(
                gameId,
                mode == QuickGameMode.CRICKET,
                legs,
                targetScore,
                doubleOut,
                masterIn,
                cutThroat
            )
        }
    }
}