// File: java/lv/mariozo/homeogo/speech/SpeechRecognizerManager.kt
// Module: HomeoGO
// Purpose: Android SpeechRecognizer wrapper exposing StateFlow<SttState>.
// Created: 16.sep.2025 22:15
// ver. 1.20 (Refactored to use SttState)
// Requires: android.permission.RECORD_AUDIO (AndroidManifest.xml)

package lv.mariozo.homeogo.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SpeechRecognizerManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())
) {

    // #2. ---- Public API & State --------------------------------------------------
    sealed interface SttState {
        data object Idle : SttState
        data object Listening : SttState
        data class Partial(val text: String) : SttState
        data class Final(val text: String) : SttState
        data class Error(val message: String) : SttState
    }

    private val _state = MutableStateFlow<SttState>(SttState.Idle)
    val state: StateFlow<SttState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null
    private val logTag = "SRM_Trace"

    fun startListening() {
        Log.d(logTag, "startListening() called")
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(logTag, "Speech recognition not available on this device.")
            emitState(SttState.Error("Runas atpazīšana nav pieejama"))
            return
        }
        ensureRecognizer()
        Log.d(logTag, "Calling recognizer.startListening() for lv-LV")
        recognizer?.startListening(buildIntent())
        // State will be updated by listener callbacks
    }

    fun stopListening() {
        Log.d(logTag, "stopListening() called")
        recognizer?.stopListening()
        // Note: `destroyRecognizer` is a more complete cleanup if not immediately restarting.
        // For now, stopListening will allow restarting. If Idle state is not reached, it might be stuck.
        // Consider if _state should be forced to Idle or an error if recognizer.stopListening() doesn't lead to onEndOfSpeech/onError.
    }

    fun destroyRecognizer() {
        Log.d(logTag, "destroyRecognizer() called")
        recognizer?.destroy()
        recognizer = null
        emitState(SttState.Idle) // Ensure state is reset
    }

    private fun ensureRecognizer() {
        if (recognizer == null) {
            Log.d(logTag, "ensureRecognizer(): Creating new SpeechRecognizer.")
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }
        } else {
            Log.d(logTag, "ensureRecognizer(): Recognizer already exists.")
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(logTag, "onReadyForSpeech (lv-LV)")
            emitState(SttState.Listening)
        }

        override fun onBeginningOfSpeech() {
            Log.d(logTag, "onBeginningOfSpeech (lv-LV)")
            // Already in Listening state, or will transition to it via onReadyForSpeech
        }

        override fun onRmsChanged(rmsdB: Float) { /* Log.d(logTag, "onRmsChanged: $rmsdB"); */ }

        override fun onBufferReceived(buffer: ByteArray?) { Log.d(logTag, "onBufferReceived (lv-LV)") }

        override fun onEndOfSpeech() {
            Log.d(logTag, "onEndOfSpeech (lv-LV)")
            // Don't change state here; wait for onResults or onError
            // If stopListening was called, we might want to go to Idle if no result/error comes soon.
        }

        override fun onError(error: Int) {
            val errorMsg = getErrorText(error)
            Log.e(logTag, "onError (lv-LV): $error - $errorMsg")
            emitState(SttState.Error(errorMsg))
            // recognizer?.cancel() // Optionally cancel to ensure it stops trying
        }

        override fun onResults(results: Bundle) {
            val resultList = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            Log.d(logTag, "onResults (lv-LV): $resultList")
            val finalText = (resultList?.firstOrNull() ?: "").trim()
            if (finalText.isNotEmpty()){
                emitState(SttState.Final(finalText))
            } else {
                 // If no result, could be an error or just no match
                emitState(SttState.Error("Nekas netika atpazīts")) // No match
            }
             // After final result, typically transition back to Idle or await further action
            // For now, it stays in Final until a new action starts listening again. 
            // Or, could go to Idle: scope.launch { emitState(SttState.Idle) }
        }

        override fun onPartialResults(partialResults: Bundle) {
            val resultList = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            Log.d(logTag, "onPartialResults (lv-LV): $resultList")
            val partialText = (resultList?.firstOrNull() ?: "").trim()
            if (partialText.isNotEmpty()) {
                emitState(SttState.Partial(partialText))
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) { Log.d(logTag, "onEvent (lv-LV): $eventType, params: $params") }
    }

    private fun buildIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "lv-LV")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // EXTRA_PROMPT is often shown by system UI, might not be needed if custom UI handles prompts
            // putExtra(RecognizerIntent.EXTRA_PROMPT, "Runā latviski …") 
        }

    private fun emitState(newState: SttState) = scope.launch {
        Log.d(logTag, "emitState: $newState")
        _state.emit(newState)
    }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio ieraksta kļūda"
            SpeechRecognizer.ERROR_CLIENT -> "Klienta puses kļūda"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Nepietiekamas atļaujas"
            SpeechRecognizer.ERROR_NETWORK -> "Tīkla kļūda"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tīkla darbības laiks ir beidzies"
            SpeechRecognizer.ERROR_NO_MATCH -> "Nekas netika atpazīts"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Atpazīšanas pakalpojums ir aizņemts"
            SpeechRecognizer.ERROR_SERVER -> "Servera kļūda"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nav runas ievades"
            else -> "Nezināma kļūda ($errorCode)"
        }
    }
}
