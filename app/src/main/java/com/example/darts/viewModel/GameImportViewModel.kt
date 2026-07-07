package com.example.darts.viewModel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.repository.DartsExportRepository
import com.example.darts.network.NearbyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

data class ImportUiState(
    val selectedFileName: String? = null,
    val selectedFileUri: Uri? = null,
    val isQrImportDetected: Boolean = false,
    val parsedPayloadText: String? = null,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ImportUiEvent {
    object OnImportCompletedSuccess : ImportUiEvent
    data class ShowToast(val msg: String) : ImportUiEvent
}

@HiltViewModel
class GameImportViewModel @Inject constructor(
    private val repository: DartsExportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private val _uiEvents = Channel<ImportUiEvent>(Channel.BUFFERED)
    val uiEvents: Flow<ImportUiEvent> = _uiEvents.receiveAsFlow()

    private var pendingRawPayload: String? = null
    private var nearbyManager: NearbyManager? = null

    // -------------------------------------------------------
    // FILE IMPORT (.darts)
    // -------------------------------------------------------
    fun processIncomingFileUri(context: Context, uri: Uri) {
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val rawContent = inputStream.bufferedReader().use { it.readText() }.trim()
                    pendingRawPayload = rawContent
                    _uiState.update { it.copy(isProcessing = false, parsedPayloadText = "[Imported File Data]") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, errorMessage = "Failed to read file.") }
            }
        }
    }

    // -------------------------------------------------------
    // NEARBY QR IMPORT (REPLACES HTTP DOWNLOAD)
    // -------------------------------------------------------
    fun processQrCodeScanResult(context: Context, token: String) {
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

        if (nearbyManager == null) nearbyManager = NearbyManager(context)

        // Start discovery to find the advertising device
        nearbyManager?.startDiscovery(
            onPayloadReceived = { payload ->
                viewModelScope.launch {
                    pendingRawPayload = payload
                    _uiState.update {
                        it.copy(
                            isQrImportDetected = true,
                            parsedPayloadText = "[Nearby Peer-to-Peer Data]",
                            isProcessing = false
                        )
                    }
                }
            },
            onStatus = { status ->
                if (status.contains("Failed")) {
                    viewModelScope.launch {
                        _uiState.update { it.copy(isProcessing = false, errorMessage = status) }
                    }
                }
            }
        )
    }

    // -------------------------------------------------------
    // IMPORT EXECUTION
    // -------------------------------------------------------
    fun executeImportConfirmation() {
        val payloadToImport = pendingRawPayload
        if (payloadToImport.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "No match data staged.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                repository.importFromQrString(payloadToImport)
                _uiEvents.send(ImportUiEvent.OnImportCompletedSuccess)
                clearImportSelection()
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, errorMessage = "Import failed.") }
            }
        }
    }

    fun clearImportSelection() {
        nearbyManager?.stopAll() // Clean up radio resources
        pendingRawPayload = null
        _uiState.update { ImportUiState() }
    }
}