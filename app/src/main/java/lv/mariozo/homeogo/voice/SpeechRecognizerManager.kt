// File: app/src/main/java/lv/mariozo/homeogo/voice/SpeechRecognizerManager.kt
// Project: HomeoGO (Android, Jetpack Compose + Material3)
// Created: 03.okt.2025 08:25 (Rīga)
// ver. 1.0
// Purpose: Wrapper around Azure Speech SDK for Speech-to-Text (STT).
// Comments:
//  - Provides startListening/stopListening/release.
//  - Exposes callbacks for partial/final results, status and errors.
//  - Uses default microphone as input.
//  - Error messages are localized to LV.

package lv.mariozo.homeogo.voice

// 1. ---- Imports ---------------------------------------------------------------
import android.content.Context
import com.microsoft.cognitiveservices.speech.*
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 2. ---- Manager ---------------------------------------------------------------
class SpeechRecognizerManager(context: Context) {

    // Replace with secure config injection (BuildConfig, env or Secrets Gradle plugin)
    private val speechConfig: SpeechConfig = SpeechConfig.fromSubscription(
        BuildConfig.AZURE_SPEECH_KEY,
        BuildConfig.AZURE_SPEECH_REGION
    ).apply {
        speechRecognitionLanguage = "lv-LV"
    }

    private var audioConfig: AudioConfig? = null
    private var recognizer: SpeechRecognizer? = null

    interface Callbacks {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onStatus(status: String)
        fun onError(messageLv: String)
    }

    fun startListening(callbacks: Callbacks) {
        stopListening()

        audioConfig = AudioConfig.fromDefaultMicrophoneInput()
        recognizer = SpeechRecognizer(speechConfig, audioConfig).apply {
            // Partial results
            recognizing.addEventListener { _, e ->
                CoroutineScope(Dispatchers.Main).launch {
                    callbacks.onPartial(e.result.text)
                }
            }
            // Final results
            recognized.addEventListener { _, e ->
                CoroutineScope(Dispatchers.Main).launch {
                    callbacks.onFinal(e.result.text)
                }
            }
            // Canceled / errors
            canceled.addEventListener { _, e ->
                CoroutineScope(Dispatchers.Main).launch {
                    callbacks.onError("STT kļūda: ${e.errorDetails}")
                }
            }
            // Session started/stopped
            sessionStarted.addEventListener { _, _ ->
                CoroutineScope(Dispatchers.Main).launch {
                    callbacks.onStatus("Sesija sākta.")
                }
            }
            sessionStopped.addEventListener { _, _ ->
                CoroutineScope(Dispatchers.Main).launch {
                    callbacks.onStatus("Sesija apturēta.")
                }
            }
        }

        recognizer?.startContinuousRecognitionAsync()
    }

    fun stopListening() {
        recognizer?.stopContinuousRecognitionAsync()
        recognizer?.close()
        recognizer = null
        audioConfig?.close()
        audioConfig = null
    }

    fun release() {
        stopListening()
        speechConfig.close()
    }
}
