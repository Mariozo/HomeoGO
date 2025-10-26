// File: java/lv/mariozo/homeogo/speech/SpeechRecognizerManager.kt
// Module: HomeoGO
// Purpose: Manager for Android SpeechRecognizer, exposes STT state as StateFlow
// Created: 16.sep.2025 22:15
// ver. 1.20 (Refactored to use SttState)
// Requires: android.permission.RECORD_AUDIO (AndroidManifest.xml)

// File: java/lv/mariozo/homeogo/speech/SpeechRecognizerManager.kt
// Module: HomeoGO
// Purpose: Manager for Android SpeechRecognizer, exposes STT state as StateFlow
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
    }

    fun stopListening() {
        Log.d(logTag, "stopListening() called")
        recognizer?.stopListening()
    }

    fun destroyRecognizer() {
        Log.d(logTag, "destroyRecognizer() called")
        recognizer?.destroy()
        recognizer = null
        emitState(SttState.Idle)
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

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            val msg = getErrorText(error)
            Log.e(logTag, "onError: $error - $msg")
            emitState(SttState.Error(msg))
        }

        override fun onResults(results: Bundle) {
            val list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val finalText = (list?.firstOrNull() ?: "").trim()
            if (finalText.isNotEmpty()) emitState(SttState.Final(finalText))
            else emitState(SttState.Error("Nekas netika atpazīts"))
        }

        override fun onPartialResults(partialResults: Bundle) {
            val list = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partialText = (list?.firstOrNull() ?: "").trim()
            if (partialText.isNotEmpty()) emitState(SttState.Partial(partialText))
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun buildIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "lv-LV")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

    private fun emitState(newState: SttState) = scope.launch {
        Log.d(logTag, "emitState: $newState")
        _state.emit(newState)
    }

    private fun getErrorText(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio ieraksta kļūda"
        SpeechRecognizer.ERROR_CLIENT -> "Klienta puses kļūda"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Nepietiekamas atļaujas"
        SpeechRecognizer.ERROR_NETWORK -> "Tīkla kļūda"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tīkla darbības laiks ir beidzies"
        SpeechRecognizer.ERROR_NO_MATCH -> "Nekas netika atpazīts"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Atpazīšanas pakalpojums ir aizņemts"
        SpeechRecognizer.ERROR_SERVER -> "Servera kļūda"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nav runas ievades"
        else -> "Nezināma kļūda ($code)"
    }
}
