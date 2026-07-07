package com.example.darts.viewModel

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.db.entities.Player
import com.example.darts.repository.DartsExportRepository
import com.example.darts.network.NearbyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface ShareUiState {
    object Idle : ShareUiState
    object Loading : ShareUiState
    data class Error(val message: String) : ShareUiState
}

data class ShareScreenState(
    val qrPayload: String? = null,
    val isGeneratingQr: Boolean = false
)

sealed interface ShareUiEvent {
    data class ShowToast(val message: String) : ShareUiEvent
    data class LaunchSystemIntent(val intent: Intent) : ShareUiEvent
}

@HiltViewModel
class GameSharingViewModel @Inject constructor(
    private val repository: DartsExportRepository
) : ViewModel() {

    val players: StateFlow<List<Player>> =
        repository.getAllPlayers()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<ShareUiState>(ShareUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _screenState = MutableStateFlow(ShareScreenState())
    val screenState = _screenState.asStateFlow()

    private val _uiEvents = Channel<ShareUiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    private var nearbyManager: NearbyManager? = null

    // -------------------------------------------------------
    // NEARBY SHARING (REPLACES WLAN SERVER)
    // -------------------------------------------------------
    fun startNearbyAdvertising(context: Context, player: Player) {
        viewModelScope.launch {
            _screenState.update { it.copy(isGeneratingQr = true) }

            try {
                val fullPayload = repository.exportPlayerToCompressedFileString(player.idPlayer)

                if (nearbyManager == null) nearbyManager = NearbyManager(context)

                nearbyManager?.startAdvertising(fullPayload) { status ->
                    viewModelScope.launch { _uiEvents.send(ShareUiEvent.ShowToast(status)) }
                }

                // QR now displays a static token that triggers discovery on the receiver side
                _screenState.update {
                    it.copy(qrPayload = "DARTS_SESSION_TOKEN", isGeneratingQr = false)
                }

            } catch (e: Exception) {
                _screenState.update { it.copy(isGeneratingQr = false) }
                _uiEvents.send(ShareUiEvent.ShowToast(e.localizedMessage ?: "Sharing failed"))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        nearbyManager?.stopAll()
    }

    // -------------------------------------------------------
    // DOWNLOAD .darts FILE
    // -------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.Q)
    fun exportToPublicDownloads(context: Context, player: Player) {
        viewModelScope.launch {
            _uiState.value = ShareUiState.Loading
            try {
                val payload = repository.exportPlayerToCompressedFileString(player.idPlayer)
                val fileName = "${player.username}_history.darts"
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/x-darts")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw Exception("Failed to create file")

                context.contentResolver.openOutputStream(uri)?.use { it.write(payload.toByteArray()) }
                _uiState.value = ShareUiState.Idle
                _uiEvents.send(ShareUiEvent.ShowToast("Export saved as .darts file"))
            } catch (e: Exception) {
                _uiState.value = ShareUiState.Error(e.localizedMessage ?: "Export failed")
            }
        }
    }
    fun stopNearbySharing() {
        nearbyManager?.stopAll()
    }

    // -------------------------------------------------------
    // WHATSAPP TEXT
    // -------------------------------------------------------
    fun shareViaWhatsAppText(player: Player) {
        viewModelScope.launch {
            try {
                val payload = repository.exportPlayerToQrString(player.idPlayer)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, payload)
                    setPackage("com.whatsapp")
                }
                _uiEvents.send(ShareUiEvent.LaunchSystemIntent(intent))
            } catch (e: Exception) {
                _uiEvents.send(ShareUiEvent.ShowToast(e.localizedMessage ?: "WhatsApp share failed"))
            }
        }
    }

    // -------------------------------------------------------
    // FILE SHARE
    // -------------------------------------------------------
    fun shareViaApplicationFile(context: Context, player: Player, targetPackage: String? = null) {
        viewModelScope.launch {
            _uiState.value = ShareUiState.Loading
            try {
                val payload = repository.exportPlayerToCompressedFileString(player.idPlayer)
                val cacheFile = File(context.cacheDir, "${player.username}_history.darts")
                cacheFile.writeText(payload)

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/x-darts"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    targetPackage?.let { setPackage(it) }
                }
                _uiEvents.send(ShareUiEvent.LaunchSystemIntent(Intent.createChooser(intent, "Share Darts File")))
                _uiState.value = ShareUiState.Idle
            } catch (e: Exception) {
                _uiState.value = ShareUiState.Error(e.localizedMessage ?: "Share failed")
            }
        }
    }
}