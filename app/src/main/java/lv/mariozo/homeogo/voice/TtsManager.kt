// File: app/src/main/java/lv/mariozo/homeogo/voice/TtsManager.kt
// Project: HomeoGO (Android, Jetpack Compose + Material3)
// Created: 03.okt.2025 08:30 (Rīga)
// ver. 1.0
// Purpose: Wrapper around Azure Speech SDK for Text-to-Speech (TTS).
// Comments:
//  - Provides speak(text) method with success callback.
//  - Uses EveritaNeural voice for Latvian output.
//  - Ensures async call, reports status back to VM.
//  - Error messages localized to LV.

package lv.mariozo.homeogo.voice

// 1. ---- Imports ---------------------------------------------------------------
import android.content.Context
import com.microsoft.cognitiveservices.speech.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 2. ---- Manager ---------------------------------------------------------------
class TtsManager(context: Context) {

    // Replace with secure config injection (BuildConfig, env or Secrets Gradle plugin)
    private val speechConfig: SpeechConfig = SpeechConfig.fromSubscription(
        BuildConfig.AZURE_SPEECH_KEY,
        BuildConfig.AZURE_SPEECH_REGION
    ).apply {
        speechSynthesisLanguage = "lv-LV"
        speechSynthesisVoiceName = "lv-LV-EveritaNeural"
    }

    private val synthesizer: SpeechSynthesizer = SpeechSynthesizer(speechConfig)

    /**
     * Speaks the given text asynchronously. Returns result to callback.
     * @param text LV string to synthesize.
     * @param onComplete true if OK, false if error.
     */
    fun speak(text: String, onComplete: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = synthesizer.SpeakTextAsync(text).get()
                val ok = result.reason == ResultReason.SynthesizingAudioCompleted
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(ok)
                }
            } catch (ex: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(false)
                }
            }
        }
    }

    fun release() {
        synthesizer.close()
        speechConfig.close()
    }
}
