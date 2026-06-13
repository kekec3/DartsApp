package com.example.darts.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.db.entities.Player
import com.example.darts.repository.DartsExportRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface ShareUiState {
    object Idle : ShareUiState
    object Loading : ShareUiState
    // Add targetPackage here (null means show generic system chooser)
    data class Success(
        val fileUri: Uri,
        val shareText: String,
        val targetPackage: String?
    ) : ShareUiState
    data class Error(val message: String) : ShareUiState
}
@HiltViewModel
class GameSharingViewModel @Inject constructor(
    private val repository: DartsExportRepository
) : ViewModel() {

    val players: StateFlow<List<Player>> = repository.getAllPlayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<ShareUiState>(ShareUiState.Idle)
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    fun prepareExport(context: Context, player: Player, targetPackage: String? = null) {
        viewModelScope.launch {
            _uiState.value = ShareUiState.Loading
            try {
                val payload = repository.getExportPayloadForPlayer(player.idPlayer)
                val jsonString = Gson().toJson(payload)

                val fileName = "${player.username.replace(" ", "_")}_history.darts"
                val cacheFile = File(context.cacheDir, fileName)
                cacheFile.writeText(jsonString)

                val authority = "${context.packageName}.fileprovider"
                val uri = FileProvider.getUriForFile(context, authority, cacheFile)

                val deepLinkMessage = "Check out my darts match history! Open it inside the app here: " +
                        "https://example.com/darts/import (Or import the attached file!)"

                // Pass the target package along to the success state
                _uiState.value = ShareUiState.Success(
                    fileUri = uri,
                    shareText = deepLinkMessage,
                    targetPackage = targetPackage
                )
            } catch (e: Exception) {
                _uiState.value = ShareUiState.Error(e.localizedMessage ?: "Failed to export data")
            }
        }
    }

    fun resetUiState() {
        _uiState.value = ShareUiState.Idle
    }
}