// File: java/lv/mariozo/homeogo/speech/SpeechRecognizerManager.kt
// Module: HomeoGO
// Purpose: Android SpeechRecognizer wrapper exposing StateFlow state.
// Created: 16.sep.2025 22:15
// ver. 1.3 (Hardcoded language to en-US for testing)
// Requires: android.permission.RECORD_AUDIO (AndroidManifest.xml)

// #1. ---- Package & Imports ---------------------------------------------------
package lv.mariozo.homeogo.speech // This is the correct package

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
// import java.util.Locale // No longer using Locale.getDefault()
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// #2. ---- Public API & State --------------------------------------------------
/**
 * SpeechRecognizerManager
 * - Encapsulates Android SpeechRecognizer.
 * - Exposes StateFlow for status and recognized text (partial/final).
 * - Provides callbacks for status changes, partial/final results, and errors.
 * - Lifecycle: start(), stop(), release().
 */
class SpeechRecognizerManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())
) {

    // #2.1 Flows (read-only for consumers)
    private val _status = MutableStateFlow("Idle")
    val status: StateFlow<String> = _status

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    // #2.2 Callbacks (optional external hooks)
    var onStatusChanged: ((String) -> Unit)? = null
    var onPartial: ((String) -> Unit)? = null
    var onFinal: ((String) -> Unit)? = null
    var onError: ((Int) -> Unit)? = null

    // #2.3 Internal
    private var recognizer: SpeechRecognizer? = null

    // #3. ---- Control ----------------------------------------------------------
    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            emitStatus("Speech recognition not available")
            return
        }
        ensureRecognizer()

        _isListening.value = true
        _text.value = ""
        emitStatus("Listening… (en-US)") // Indicate test language

        recognizer?.startListening(buildIntent())
    }

    fun stop() {
        _isListening.value = false
        recognizer?.stopListening()
        recognizer?.cancel()
        emitStatus("Idle")
    }

    fun release() {
        _isListening.value = false
        recognizer?.destroy()
        recognizer = null
        emitStatus("Released")
    }

    // #4. ---- Helpers ----------------------------------------------------------
    private fun ensureRecognizer() {
        if (recognizer != null) return
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { emitStatus("Listening… (en-US)") }
                override fun onBeginningOfSpeech() { emitStatus("Speak! (en-US)") }
                override fun onRmsChanged(rmsdB: Float) { /* ignore */ }
                override fun onBufferReceived(buffer: ByteArray?) { /* ignore */ }
                override fun onEndOfSpeech() { emitStatus("Processing… (en-US)") }
                override fun onError(error: Int) {
                    _isListening.value = false
                    emitStatus("SRM_ON_ERROR_CODE: $error") // More specific diagnostic for onError
                    onError?.invoke(error) // Specific error callback
                }
                override fun onResults(results: Bundle) {
                    _isListening.value = false
                    val list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val finalText = (list?.firstOrNull() ?: "").trim()
                    emitStatus("SRM_FINAL_RAW: $finalText") // Diagnostic
                    emitText(finalText) // Update StateFlow
                    onFinal?.invoke(finalText) // Invoke onFinal callback
                    // Final status is emitted via emitStatus, which will call onStatusChanged
                    // emitStatus(if (finalText.isNotEmpty()) "Idle" else "Nothing recognized") // Original status update deferred to ViewModel
                }
                override fun onPartialResults(partialResults: Bundle) {
                    val list = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = (list?.firstOrNull() ?: "").trim()
                    if (partial.isNotEmpty()) {
                        emitStatus("SRM_PARTIAL_RAW: $partial") // Diagnostic
                        emitText(partial) // Update StateFlow
                        onPartial?.invoke(partial) // Invoke onPartial callback
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) { /* ignore */ }
            })
        }
    }

    private fun buildIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()) // Using hardcoded language for testing
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "lv-LV")  // Hardcoded to English (US) for test
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Runā latviski …")
        }

    private fun emitStatus(s: String) = scope.launch {
        _status.emit(s)
        onStatusChanged?.invoke(s) // Invoke onStatusChanged callback
    }

    private fun emitText(t: String) = scope.launch { _text.emit(t) }
}
