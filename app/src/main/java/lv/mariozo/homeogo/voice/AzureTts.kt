

// File: app/src/main/java/lv/mariozo/homeogo/voice/AzureTts.kt
// Module: HomeoGO
// Purpose: Wrapper for Azure Cognitive Services TTS (blocking call)
// Created: 20.sep.2025 15:35
// ver. 1.0

package lv.mariozo.homeogo.voice

import android.content.Context
import android.widget.Toast
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import lv.mariozo.homeogo.BuildConfig


class AzureTts(context: Context) : AutoCloseable {

    private val ctx = context.applicationContext

    private val speechConfig: SpeechConfig =
        SpeechConfig.fromSubscription(
            BuildConfig.AZURE_SPEECH_KEY,
            BuildConfig.AZURE_SPEECH_REGION
        ).apply {
            speechSynthesisLanguage = "lv-LV"
            speechSynthesisVoiceName = "lv-LV-EveritaNeural"
        }

    private val audioConfig: AudioConfig = AudioConfig.fromDefaultSpeakerOutput()
    private val synthesizer: SpeechSynthesizer = SpeechSynthesizer(speechConfig, audioConfig)

    fun speakBlocking(text: String) {
        if (text.isBlank()) return
        try {
            Toast.makeText(ctx,
                "Azure voice: ${speechConfig.speechSynthesisVoiceName}",
                Toast.LENGTH_LONG).show()

            val result: SpeechSynthesisResult = synthesizer.SpeakText(text)

            when (result.reason) {
                ResultReason.SynthesizingAudioCompleted -> {
                    Toast.makeText(ctx, "Azure TTS: completed (Everita)", Toast.LENGTH_SHORT).show()
                }
                ResultReason.Canceled -> {
                    val details = SpeechSynthesisCancellationDetails.fromResult(result)
                    Toast.makeText(ctx, "Azure TTS canceled: ${details.errorDetails}", Toast.LENGTH_LONG).show()
                }
                else -> {
                    Toast.makeText(ctx, "Azure TTS: result=$result", Toast.LENGTH_SHORT).show()
                }
            }
            result.close()
        } catch (t: Throwable) {
            Toast.makeText(ctx, "Azure TTS error: ${t.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun close() {
        synthesizer.close()
        audioConfig.close()
        speechConfig.close()
    }
}
