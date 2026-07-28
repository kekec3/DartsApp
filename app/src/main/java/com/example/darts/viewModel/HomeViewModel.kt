package com.example.darts.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.db.repositories.BattleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val battleRepository: BattleRepository
) : ViewModel() {

    fun onStartGameClicked(
        onNavigateToGameSettings: (Int) -> Unit,
        onNavigateToBattles: () -> Unit
    ) {
        viewModelScope.launch {
            val battles = battleRepository.getAllBattles().firstOrNull() ?: emptyList()

            // Get the battle with the highest timestamp/ID
            val latestBattle = battles.maxByOrNull {
                it.dateCreated.toLongOrNull() ?: it.idBattle.toLong()
            }

            if (latestBattle != null) {
                onNavigateToGameSettings(latestBattle.idBattle)
            } else {
                onNavigateToBattles()
            }
        }
    }
}