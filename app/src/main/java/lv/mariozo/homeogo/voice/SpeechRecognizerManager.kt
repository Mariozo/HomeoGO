// File: app/src/main/java/lv/mariozo/homeogo/voice/SpeechRecognizerManager.kt
// Project: HomeoGO
// Created: 03.okt.2025 11:55 (Rīga)
// ver. 1.1
// Purpose: Azure Speech SDK Speech-to-Text wrapper for HomeoGO.
// Comments:
//  - Constructor-injected key/region (avoid direct BuildConfig dependency).
//  - Provides startListening/stopListening/release APIs.
//  - Exposes callbacks for partial/final results, status updates, and errors.
//  - Uses default microphone as input and Latvian language.

package lv.mariozo.homeogo.voice

// 1. ---- Imports ---------------------------------------------------------------
import android.content.Context
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 2. ---- Manager ---------------------------------------------------------------
class SpeechRecognizerManager(
    private val context: Context,
    speechKey: String,
    speechRegion: String,
    language: String = "lv-LV",
) {

    private val speechConfig: SpeechConfig =
        SpeechConfig.fromSubscription(speechKey, speechRegion).apply {
            speechRecognitionLanguage = language
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
            recognizing.addEventListener { _, e ->
                CoroutineScope(Dispatchers.Main).launch {
                    callbacks.onPartial(e.result.text ?: "")
                }
            }
            recognized.addEventListener { _, e ->
                CoroutineScope(Dispatchers.Main).launch {
                    callbacks.onFinal(e.result.text ?: "")
                }
            }
            canceled.addEventListener { _, e ->
                CoroutineScope(Dispatchers.Main).launch {
                    callbacks.onError("STT kļūda: ${e.errorDetails}")
                }
            }
            sessionStarted.addEventListener { _, _ ->
                CoroutineScope(Dispatchers.Main).launch {
                    callbacks.onStatus("Sesija sākta")
                }
            }
            sessionStopped.addEventListener { _, _ ->
                CoroutineScope(Dispatchers.Main).launch {
                    callbacks.onStatus("Sesija apturēta")
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
