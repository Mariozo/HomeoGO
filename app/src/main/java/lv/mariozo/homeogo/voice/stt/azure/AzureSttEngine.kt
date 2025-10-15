package lv.mariozo.homeogo.voice.stt.azure

import android.content.Context
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig

class AzureSttEngine(
    context: Context,
    key: String,
    region: String,
    language: String = "lv-LV",
) {
    private val speechConfig: SpeechConfig = SpeechConfig.fromSubscription(key, region).apply {
        speechRecognitionLanguage = language
    }

    private var recognizer: SpeechRecognizer? = null

    fun start(
        onResult: (String) -> Unit,
        onError: (Throwable) -> Unit = {},
    ) {
        stop() // drošībai
        val audioCfg = AudioConfig.fromDefaultMicrophoneInput()
        recognizer = SpeechRecognizer(speechConfig, audioCfg).apply {
            recognizing.addEventListener { _, e -> /* starprezultāts: e.result.text */ }
            recognized.addEventListener { _, e ->
                val txt = e.result?.text.orEmpty()
                if (txt.isNotBlank()) onResult(txt)
            }
            canceled.addEventListener { _, e ->
                onError(IllegalStateException("STT canceled: ${e.errorCode} ${e.errorDetails}"))
            }
            sessionStopped.addEventListener { _, _ -> /* no-op */ }
        }
        try {
            recognizer?.startContinuousRecognitionAsync()?.get()
        } catch (t: Throwable) {
            onError(t)
        }
    }

    fun stop() {
        try {
            recognizer?.stopContinuousRecognitionAsync()?.get()
        } catch (_: Throwable) { /* ignore */
        }
        recognizer?.close()
        recognizer = null
    }
}