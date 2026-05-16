package com.example.darts.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.darts.engine.DartThrow
import com.example.darts.engine.Multiplier
import com.example.darts.utils.CommandParser

private const val TAG = "VoiceInputManager"

data class VoiceUiState(
    val status: VoiceStatus   = VoiceStatus.IDLE,
    val transcript: String    = "",
    val feedback: String      = "",
    val isError: Boolean      = false,
    val errorHint: String     = ""
)

enum class VoiceStatus { IDLE, LISTENING, PROCESSING }

class VoiceInputManager(
    private val context: Context,
    private val parser: CommandParser = CommandParser(),
    private val onCommand: (CommandParser.VoiceCommand) -> Unit
) {
    var uiState by mutableStateOf(VoiceUiState())
        private set

    private var recognizer: SpeechRecognizer? = null
    private var autoRestart = true

    init {
        Log.d(TAG, "=== VoiceInputManager created ===")
    }

    fun start() {
        autoRestart = true
        buildAndStart()
    }

    fun stop() {
        autoRestart = false
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
        uiState = uiState.copy(status = VoiceStatus.IDLE)
    }

    fun destroy() {
        autoRestart = false
        recognizer?.destroy()
        recognizer = null
    }

    private fun buildAndStart() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(listener)
            startListening(recognizerIntent())
        }
        uiState = uiState.copy(status = VoiceStatus.LISTENING, isError = false, errorHint = "")
    }

    private fun recognizerIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)

        // TIP: If you want to speak English, leave en-US or try en-GB.
        // If you actually want to speak your native language, change this to your locale (e.g., "sr-RS" or "ru-RU")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")

        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10)

        // ⭐ CHANGED: Must be true to see real-time what the engine is hearing
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

        // ⭐ FIXED: Changed 60000L to 2500L (2.5 seconds) so it doesn't hang forever
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 200L)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            uiState = uiState.copy(status = VoiceStatus.LISTENING, transcript = "")
        }

        override fun onBeginningOfSpeech() {
            uiState = uiState.copy(transcript = "…")
        }

        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onResults(results: Bundle) {
            uiState = uiState.copy(status = VoiceStatus.PROCESSING)

            val candidates = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()

            // ⭐ HIGH VISIBILITY LOGGING ⭐
            // Using Log.e so it shows up bright RED in logcat, making it easy to spot your mispronunciations
            Log.e(TAG, "========================================")
            Log.e(TAG, "🎯 FINAL RESULTS FOR PARSER 🎯")
            candidates.forEachIndexed { i, text ->
                Log.e(TAG, "  -> Option $i: \"$text\"")
            }
            Log.e(TAG, "========================================")

            val heard = candidates.firstOrNull() ?: ""
            val command = parser.parse(candidates)

            val feedback = when (command) {
                is CommandParser.VoiceCommand.Throw  -> command.dart.toFeedbackString()
                is CommandParser.VoiceCommand.Undo   -> "↩ Undo"
                is CommandParser.VoiceCommand.Submit -> "✓ Submit"
                is CommandParser.VoiceCommand.Unknown -> "? \"$heard\""
            }

            uiState = uiState.copy(
                transcript = heard,
                feedback   = feedback,
                isError    = command is CommandParser.VoiceCommand.Unknown
            )

            if (command !is CommandParser.VoiceCommand.Unknown) {
                onCommand(command)
            }

            restartIfNeeded()
        }

        override fun onError(error: Int) {
            // Ignore ERROR_NO_MATCH (7) and ERROR_SPEECH_TIMEOUT (6) for silent restarts
            if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                Log.w(TAG, "❌ Error: $error")
            }
            uiState = uiState.copy(isError = true)
            restartIfNeeded()
        }

        override fun onPartialResults(partial: Bundle?) {
            val text = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
            if (text.isNotEmpty()) {
                // Using Log.w so partial results show up ORANGE in logcat
                Log.w(TAG, "🗣️ HEARING (Partial): \"$text\"")
            }
        }

        override fun onEvent(type: Int, params: Bundle?) {}
    }

    private fun restartIfNeeded() {
        if (autoRestart) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (autoRestart) buildAndStart()
            }, 500L) // Sped up restart slightly for better flow
        }
    }

    private fun DartThrow.toFeedbackString(): String {
        val prefix = when (multiplier) {
            Multiplier.SINGLE -> if (value == 0) "MISS" else "S"
            Multiplier.DOUBLE -> "D"
            Multiplier.TRIPLE -> "T"
        }
        val label = if (value == 0) "" else "$value"
        return "$prefix$label  →  ${score()} pts"
    }
}