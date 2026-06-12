package com.example.darts.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.db.entities.Player
import com.example.darts.repository.DartsExportRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

sealed interface ShareUiState {
    object Idle : ShareUiState
    object Loading : ShareUiState
    data class Success(val fileUri: Uri, val shareText: String) : ShareUiState
    data class Error(val message: String) : ShareUiState
}

class GameSharingViewModel(
    private val repository: DartsExportRepository
) : ViewModel() {

    val players: StateFlow<List<Player>> = repository.getAllPlayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<ShareUiState>(ShareUiState.Idle)
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    fun prepareExport(context: Context, player: Player) {
        viewModelScope.launch {
            _uiState.value = ShareUiState.Loading
            try {
                val payload = repository.getExportPayloadForPlayer(player.idPlayer)

                // Serialize to JSON
                val jsonString = Gson().toJson(payload)

                // Write to cache directory with custom extension
                val fileName = "${player.username.replace(" ", "_")}_history.darts"
                val cacheFile = File(context.cacheDir, fileName)
                cacheFile.writeText(jsonString)

                // Generate safe share URI matching your FileProvider authority
                val authority = "${context.packageName}.fileprovider"
                val uri = FileProvider.getUriForFile(context, authority, cacheFile)

                val deepLinkMessage = "Check out my darts match history! Open it inside the app here: " +
                        "https://example.com/darts/import (Or import the attached file!)"

                _uiState.value = ShareUiState.Success(fileUri = uri, shareText = deepLinkMessage)
            } catch (e: Exception) {
                _uiState.value = ShareUiState.Error(e.localizedMessage ?: "Failed to export data")
            }
        }
    }

    fun resetUiState() {
        _uiState.value = ShareUiState.Idle
    }
}