package com.example.darts.ui.viewmodels

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
    data class Success(
        val fileUri: Uri,
        val shareText: String,
        val targetPackage: String?,
        val isSavedToDisk: Boolean = false
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

    fun exportToPublicDownloads(context: Context, player: Player) {
        viewModelScope.launch {
            _uiState.value = ShareUiState.Loading
            try {
                val payload = repository.getExportPayloadForPlayer(player.idPlayer)
                val jsonString = Gson().toJson(payload)
                val fileName = "${player.username.replace(" ", "_")}_history.darts"

                val resolver = context.contentResolver

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                }

                val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                }

                val fileUri = resolver.insert(collectionUri, contentValues)
                    ?: throw Exception("Failed to open the standard Downloads repository slot")

                resolver.openOutputStream(fileUri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }

                _uiState.value = ShareUiState.Success(
                    fileUri = fileUri,
                    shareText = "Exported file directly to local disk space!",
                    targetPackage = null,
                    isSavedToDisk = true
                )
            } catch (e: Exception) {
                _uiState.value = ShareUiState.Error(e.localizedMessage ?: "Failed to save file disk backup")
            }
        }
    }

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
                val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, cacheFile)

                val deepLinkMessage = "Check out my darts match history! Open it inside the app here: " +
                        "https://example.com/darts/import"

                _uiState.value = ShareUiState.Success(
                    fileUri = uri,
                    shareText = deepLinkMessage,
                    targetPackage = targetPackage,
                    isSavedToDisk = false
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