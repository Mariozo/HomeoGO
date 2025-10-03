// File: app/src/main/java/lv/mariozo/homeogo/voice/TtsManager.kt
// Project: HomeoGO
// Created: 03.okt.2025 11:50 (Rīga)
// ver. 1.3
// Purpose: Azure Speech SDK Text-to-Speech wrapper without direct BuildConfig dependency.
//          Keys/region are injected via constructor to avoid IDE/gradle generation timing issues.
// Comments:
//  - Pass values from caller (e.g., MainActivity/ElzaViewModel) using BuildConfig.* there.
//  - Default Latvian voice: lv-LV-EveritaNeural. Switch if you need a different voice.

package lv.mariozo.homeogo.voice

// 1. ---- Imports ---------------------------------------------------------------
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// 2. ---- Manager ---------------------------------------------------------------
class TtsManager(
    speechKey: String,
    speechRegion: String,
    voiceName: String = "lv-LV-EveritaNeural",
) {

    // 2.1 ---- Speech configuration (constructor-injected) ----------------------
    private val speechConfig: SpeechConfig = SpeechConfig.fromSubscription(
        speechKey,
        speechRegion
    ).apply {
        speechSynthesisLanguage = "lv-LV"
        speechSynthesisVoiceName = voiceName
    }

    private val synthesizer: SpeechSynthesizer = SpeechSynthesizer(speechConfig)
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    /**
     * Synthesize speech for the given text asynchronously.
     * @param text Text to speak (LV).
     * @param onComplete Callback on main thread: true if success, false if error.
     */
    fun speak(text: String, onComplete: (Boolean) -> Unit) {
        scope.launch {
            runCatching {
                val result = synthesizer.SpeakTextAsync(text).get()
                result.reason == ResultReason.SynthesizingAudioCompleted
            }.onSuccess { ok ->
                kotlinx.coroutines.withContext(Dispatchers.Main) { onComplete(ok) }
            }.onFailure {
                kotlinx.coroutines.withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    // 2.2 ---- Cleanup ----------------------------------------------------------
    fun release() {
        runCatching { synthesizer.close() }
        runCatching { speechConfig.close() }
    }
}
