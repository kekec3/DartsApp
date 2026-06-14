package com.example.darts.viewModel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darts.repository.DartsExportRepository
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
    val parsedPayloadText: String? = null, // Stores the descriptive preview string
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

    // Holds the raw Base64+GZIP string waiting for database confirmation
    private var pendingRawPayload: String? = null

    // -------------------------------------------------------
    // FILE IMPORT (.darts)
    // -------------------------------------------------------
    fun processIncomingFileUri(context: Context, uri: Uri) {
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

        var fileName: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }

        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        val rawContent = reader.readText().trim()

                        if (rawContent.isBlank()) {
                            throw IllegalArgumentException("The selected file is empty.")
                        }

                        pendingRawPayload = rawContent

                        _uiState.update {
                            it.copy(
                                selectedFileUri = uri,
                                selectedFileName = fileName ?: "Imported File",
                                isQrImportDetected = false,
                                parsedPayloadText = "[Compressed Darts Match File Data]",
                                isProcessing = false,
                                errorMessage = null
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Failed to read file contents."
                    )
                }
            }
        }
    }

    // -------------------------------------------------------
    // QR IMPORT
    // -------------------------------------------------------
    fun processQrCodeScanResult(rawResult: String) {
        if (rawResult.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Scanned QR code is empty.") }
            return
        }

        // Stage the raw, encoded Base64-GZIP text directly
        pendingRawPayload = rawResult.trim()

        _uiState.update {
            it.copy(
                selectedFileUri = null,
                selectedFileName = null,
                isQrImportDetected = true,
                parsedPayloadText = "[Compressed QR Code Match Data]",
                errorMessage = null
            )
        }
    }

    // -------------------------------------------------------
    // IMPORT EXECUTION
    // -------------------------------------------------------
    fun executeImportConfirmation() {
        val payloadToImport = pendingRawPayload
        if (payloadToImport.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "No match data staged for import.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

            try {
                // Pass the raw string payload down to the repository.
                // importFromQrString handles decoding Base64 AND decompressing GZIP!
                repository.importFromQrString(payloadToImport)

                _uiState.update { it.copy(isProcessing = false) }
                _uiEvents.send(ImportUiEvent.OnImportCompletedSuccess)
                clearImportSelection()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Import failed: Corrupted payload or matching record already exists."
                    )
                }
            }
        }
    }

    fun clearImportSelection() {
        pendingRawPayload = null
        _uiState.update { ImportUiState() }
    }
}