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

package lv.mariozo.homeogo.voice

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

/**
 * Vienkāršs Android SpeechRecognizer ietinamais menedžeris ar StateFlow stāvokli.
 * Paredzēts ElzaViewModel vajadzībām.
 */
class SpeechRecognizerManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())
) {

    // ---- Publiskais stāvoklis -------------------------------------------------------
    sealed interface SttState {
        data object Idle : SttState
        data object Listening : SttState
        data class Partial(val text: String) : SttState
        data class Final(val text: String) : SttState
        data class Error(val message: String) : SttState
    }

    private val _state = MutableStateFlow<SttState>(SttState.Idle)
    val state: StateFlow<SttState> = _state.asStateFlow()

    // ---- Iekšējie lauki -------------------------------------------------------------
    private var recognizer: SpeechRecognizer? = null
    private val logTag = "SRM_Trace"

    // Rezervēts VAD (lai var piesiet vēlāk)
    private var vadSensitivity: Float = 0.5f
    fun setVadSensitivity(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        vadSensitivity = clamped
        Log.d(logTag, "VAD sensitivity set to $vadSensitivity")
    }

    // ---- Publiskais API --------------------------------------------------------------
    fun startListening() {
        Log.d(logTag, "startListening()")
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(logTag, "Speech recognition not available on this device.")
            emitState(SttState.Error("Runas atpazīšana nav pieejama"))
            return
        }
        ensureRecognizer()
        recognizer?.startListening(buildIntent())
        // Stāvokli atjaunina klausītājs
    }

    fun stopListening() {
        Log.d(logTag, "stopListening()")
        recognizer?.stopListening()
        // Atstājam klausītājam izšķirt galīgo stāvokli (onResults/onError)
    }

    fun destroyRecognizer() {
        Log.d(logTag, "destroyRecognizer()")
        recognizer?.destroy()
        recognizer = null
        emitState(SttState.Idle)
    }

    // ---- Palīgfunkcijas --------------------------------------------------------------
    private fun ensureRecognizer() {
        if (recognizer == null) {
            Log.d(logTag, "ensureRecognizer(): creating new SpeechRecognizer")
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(logTag, "onReadyForSpeech")
            emitState(SttState.Listening)
        }

        override fun onBeginningOfSpeech() {
            Log.d(logTag, "onBeginningOfSpeech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Log.v(logTag, "onRmsChanged: $rmsdB")
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            Log.d(logTag, "onBufferReceived")
        }

        override fun onEndOfSpeech() {
            Log.d(logTag, "onEndOfSpeech")
        }

        override fun onError(error: Int) {
            val msg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio ieraksta kļūda"
                SpeechRecognizer.ERROR_CLIENT -> "Klienta puses kļūda"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Nepietiekamas atļaujas"
                SpeechRecognizer.ERROR_NETWORK -> "Tīkla kļūda"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tīkla noildze"
                SpeechRecognizer.ERROR_NO_MATCH -> "Nekas netika atpazīts"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Atpazinējs aizņemts"
                SpeechRecognizer.ERROR_SERVER -> "Servera kļūda"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nav runas"
                else -> "Nezināma kļūda ($error)"
            }
            Log.e(logTag, "onError: $error ($msg)")
            emitState(SttState.Error(msg))
        }

        override fun onResults(results: Bundle) {
            val list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = (list?.firstOrNull() ?: "").trim()
            Log.d(logTag, "onResults: $text")
            if (text.isNotEmpty()) {
                emitState(SttState.Final(text))
            } else {
                emitState(SttState.Error("Nekas netika atpazīts"))
            }
        }

        override fun onPartialResults(partialResults: Bundle) {
            val list = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = (list?.firstOrNull() ?: "").trim()
            if (text.isNotEmpty()) {
                emitState(SttState.Partial(text))
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            Log.d(logTag, "onEvent: $eventType, $params")
        }
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
        _state.emit(newState)
    }
}
