

// File: app/src/main/java/lv/mariozo/homeogo/voice/AzureTts.kt
// Module: HomeoGO
// Purpose: Wrapper for Azure Cognitive Services TTS (blocking call)
// Created: 20.sep.2025 14:25
// ver. 1.0

package lv.mariozo.homeogo.voice

import android.content.Context
import com.microsoft.cognitiveservices.speech.CancellationDetails
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import lv.mariozo.homeogo.BuildConfig

class AzureTts(context: Context) : AutoCloseable {
    private val speechConfig: SpeechConfig =
        SpeechConfig.fromSubscription(
            BuildConfig.AZURE_SPEECH_KEY,
            BuildConfig.AZURE_SPEECH_REGION
        )

    private val audioConfig: AudioConfig = AudioConfig.fromDefaultSpeakerOutput()
    private val synthesizer: SpeechSynthesizer =
        SpeechSynthesizer(speechConfig, audioConfig)

    fun speakBlocking(text: String) {
        if (text.isBlank()) return
        try {
            // Synchronous call (no futures, no .get())
            val result = synthesizer.SpeakText(text)

            when (result.reason) {
                ResultReason.SynthesizingAudioCompleted -> {
                    // OK
                }
                ResultReason.Canceled -> {
                    val details = CancellationDetails.fromResult(result)
                    // (pagaidām neko nedaram; ja vajadzēs, varam logot details.errorDetails)
                }
                else -> { /* ignore */ }
            }
            result.close()
        } catch (t: Throwable) {
            // Klusa aizsardzība; ja vajadzēs — iemetīsim logu/Toast
        }
    }

    override fun close() {
        synthesizer.close()
        audioConfig.close()
        speechConfig.close()
    }
}
