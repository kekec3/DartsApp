package com.example.darts.viewModel

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.repository.DartsExportRepository
import com.example.darts.network.NearbyManager
import com.google.gson.JsonSyntaxException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.EOFException
import java.util.zip.ZipException
import javax.inject.Inject

private const val TAG = "IMPORT"

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

    /** Token we are already listening for, so a repeated scan is a no-op. */
    private var activeDiscoveryToken: String? = null

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
        // Basic sanity check to ensure we scanned a Darts app QR
        if (!token.startsWith("DARTS_")) {
            _uiState.update { it.copy(errorMessage = "Invalid QR Code scanned.") }
            return
        }

        // Scanning the same code again must not restart discovery.
        if (activeDiscoveryToken == token) return
        activeDiscoveryToken = token

        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

        if (nearbyManager == null) nearbyManager = NearbyManager(context)

        // Start discovery matching ONLY the token scanned from the QR
        nearbyManager?.startDiscovery(
            targetToken = token,
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
                    //_uiEvents.send(ImportUiEvent.ShowToast("Data received, confirm to import!"))
                }
            },
            onStatus = { status ->
                viewModelScope.launch {
                    if (status.contains("Failed") || status.contains("Denied")) {
                        // Let the user retry the scan after a genuine failure.
                        activeDiscoveryToken = null
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
                // A bare "Import failed." hides which stage broke. Report the stage and
                // keep the full stack trace in logcat under the IMPORT tag.
                Log.e(
                    TAG,
                    "Import failed. Staged payload length=${payloadToImport.length} chars, " +
                        "starts with='${payloadToImport.take(24)}'",
                    e
                )
                _uiState.update {
                    it.copy(isProcessing = false, errorMessage = describeImportFailure(e))
                }
            }
        }
    }

    /**
     * Maps the exception to the pipeline stage that produced it:
     * Base64 decode → GZIP inflate → Gson parse → Room insert.
     */
    private fun describeImportFailure(e: Exception): String = when (e) {
        is IllegalArgumentException ->
            "Data is not valid Base64 — the transfer was probably truncated."
        is ZipException, is EOFException ->
            "Data is incomplete or corrupted — the transfer did not finish."
        is JsonSyntaxException ->
            "Decoded data is not a valid Darts export."
        is SQLiteConstraintException ->
            "Database rejected the import: it conflicts with data already on this device."
        is SQLiteException ->
            "Database error while importing: ${e.message}"
        is NullPointerException ->
            "Export payload is missing required sections."
        else ->
            "Import failed: ${e::class.java.simpleName}: ${e.message ?: "no detail"}"
    }

    fun clearImportSelection() {
        nearbyManager?.stopAll() // Clean up radio resources
        pendingRawPayload = null
        activeDiscoveryToken = null
        _uiState.update { ImportUiState() }
    }

    override fun onCleared() {
        super.onCleared()
        nearbyManager?.stopAll()
    }
}