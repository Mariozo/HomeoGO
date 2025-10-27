// ============================================================================
// File: app/src/main/java/lv/mariozo/homeogo/voice/tts/azure/AzureTtsEngine.kt
// Module: HomeoGO
// Purpose: Azure Cognitive Services TTS engine implementation (Everita voice)
// Created: 27.sep.2025 20:00 (Europe/Riga)
// ver. 1.2 (fixed speakTextAsync + result.reason)
// ============================================================================

package lv.mariozo.homeogo.voice.tts.azure

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lv.mariozo.homeogo.voice.tts.TtsEngine
import java.io.File
import java.io.FileInputStream
import java.util.UUID

class AzureTtsEngine(
    private val context: Context,
    private val key: String,
    private val region: String,
    private val voice: String = "lv-LV-EveritaNeural",
) : TtsEngine {

    override fun name() = "AzureTTS"

    private val speechConfig: SpeechConfig by lazy {
        SpeechConfig.fromSubscription(key, region).apply {
            // Nav obligāti, bet korekti:
            // speechSynthesisLanguage = "lv-LV"
            speechSynthesisVoiceName = voice
        }
    }

    override suspend fun speak(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        // 1) Audio fokus (atsevišķi emu tas ir kritiski)
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.requestAudioFocus(
            null, AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        )

        // 2) Ģenerējam WAV uz /cache ar unikālu nosaukumu
        val wavFile = File(context.cacheDir, "azure_tts_${UUID.randomUUID()}.wav")

        try {
            // 3) Azure → WAV fails
            val audioCfg = AudioConfig.fromWavFileOutput(wavFile.absolutePath)
            val synthResult: SpeechSynthesisResult =
                SpeechSynthesizer(speechConfig, audioCfg).use { synth ->
                    // SDK versijām, kur nav SpeakTextAsync – izmantojam SpeakText
                    synth.SpeakText(text)
                }

            if (synthResult.reason != ResultReason.SynthesizingAudioCompleted) {
                val c = SpeechSynthesisCancellationDetails.fromResult(synthResult)
                return@withContext Result.failure(
                    IllegalStateException("Azure CANCELED: ${c.errorCode} ${c.errorDetails}")
                )
            }

            // 4) Verificē, ka tiešām ir audio
            val bytes = wavFile.length()
            if (!wavFile.exists() || bytes <= 44L) { // ~ WAV galvene 44 baiti
                return@withContext Result.failure(IllegalStateException("Audio file empty (${bytes}B)"))
            }

            // 5) Atskaņo uz UI pavediena ar MediaPlayer (stabilāk emu + device)
            withContext(Dispatchers.Main) {
                val mp = MediaPlayer()
                if (Build.VERSION.SDK_INT >= 21) {
                    mp.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
                }
                FileInputStream(wavFile).use { fis ->
                    mp.setDataSource(fis.fd)
                    mp.setOnPreparedListener { it.start() }
                    mp.setOnCompletionListener { it.release(); /* wavFile.delete() */ }
                    mp.setOnErrorListener { player, what, extra ->
                        player.release()
                        false
                    }
                    mp.prepareAsync()
                }
            }

            // 6) Atgriežam OK tikai tad, ja tiešām uzģenerēts audio
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            am.abandonAudioFocus(null)
        }
    }
}
