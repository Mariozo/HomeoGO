// File: app/src/main/java/lv/mariozo/homeogo/voice/TtsManager.kt
// Project: HomeoGO
// Created: 03.okt.2025 11:50 (Rīga)
// ver. 1.5
// Purpose: Azure Speech SDK Text-to-Speech wrapper (PCM 16 kHz output).
// Comments:
//  - Added 16 kHz PCM output to reduce resampling distortion in emulator.
//  - Keeps full playback on physical devices and barge-in compatibility.

package lv.mariozo.homeogo.voice

// 1. ---- Imports ---------------------------------------------------------------
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        // Added fixed PCM 16 kHz output for emulator stability
        setSpeechSynthesisOutputFormat(
            SpeechSynthesisOutputFormat.Riff16Khz16BitMonoPcm
        )
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
                withContext(Dispatchers.Main) { onComplete(ok) }
            }.onFailure {
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    /**
     * Immediately stops any ongoing speech synthesis.
     */
    fun stop() {
        scope.launch {
            runCatching {
                synthesizer.StopSpeakingAsync().get()
            }
        }
    }

    // 2.2 ---- Cleanup ----------------------------------------------------------
    fun release() {
        runCatching { synthesizer.close() }
        runCatching { speechConfig.close() }
    }
}
