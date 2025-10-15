// ============================================================================
// File: app/src/main/java/lv/mariozo/homeogo/voice/tts/azure/AzureTtsEngine.kt
// Module: HomeoGO  (fixed speakTextAsync + result.reason)
// Created: 28.sep.2025 10:45
// ver. 1.3
// Purpose: Azure Cognitive Services TTS (Everita) ar SSML <prosody rate="…">
// Notes:  If rate == 1.0f → SpeakText; otherwise → SpeakSsml (with prosody rate).
//         Audio output to WAV and played with MediaPlayer (stable emu + device).
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
import java.util.Locale
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
            // var izmantot arī speechSynthesisLanguage = "lv-LV"
            speechSynthesisVoiceName = voice
        }
    }

    // Publiskā API: vienkāršā izsaukuma ceļš
    override suspend fun speak(text: String): Result<Unit> =
        speakWithRate(text = text, rate = 1.0f)

    // Publiskā API: SSML ar ātrumu
    suspend fun speakSsml(text: String, rate: Float): Result<Unit> =
        speakWithRate(text = text, rate = rate)

    // Iekšējais ceļš: ja rate==1 → SpeakText; citādi → SpeakSsml
    private suspend fun speakWithRate(text: String, rate: Float): Result<Unit> =
        withContext(Dispatchers.IO) {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )

            // unikāls WAV fails kešā
            val wavFile = File(context.cacheDir, "azure_tts_${UUID.randomUUID()}.wav")

            try {
                val audioCfg = AudioConfig.fromWavFileOutput(wavFile.absolutePath)
                val synthesisResult: SpeechSynthesisResult =
                    SpeechSynthesizer(speechConfig, audioCfg).use { synth ->
                        if (rate.isOne()) {
                            // sinhronais ceļš: SpeakText (tavā SDK pieejams)
                            synth.SpeakText(text)
                        } else {
                            val ssml = buildSsml(text = text, rate = rate, voice = voice)
                            synth.SpeakSsml(ssml)
                        }
                    }

                if (synthesisResult.reason != ResultReason.SynthesizingAudioCompleted) {
                    val c = SpeechSynthesisCancellationDetails.fromResult(synthesisResult)
                    return@withContext Result.failure(
                        IllegalStateException("Azure CANCELED: ${c.errorCode} ${c.errorDetails}")
                    )
                }

                // pārbaude, ka WAV tiešām ir
                if (!wavFile.exists() || wavFile.length() <= 44L) {
                    return@withContext Result.failure(IllegalStateException("Audio file empty (${wavFile.length()}B)"))
                }

                // Atskaņošana uz UI pavediena
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
                        mp.setOnCompletionListener { player ->
                            player.release()
                            // ja gribi, vari dzēst: wavFile.delete()
                        }
                        mp.setOnErrorListener { player, _, _ -> player.release(); false }
                        mp.prepareAsync()
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                am.abandonAudioFocus(null)
            }
        }

    private fun buildSsml(text: String, rate: Float, voice: String): String {
        // Normalizējam 0.5..1.5; Azure SSML 'rate' pieņem %, 'x-slow'..'x-fast', u.c.
        val clamped = rate.coerceIn(0.5f, 1.5f)
        val percent = ((clamped - 1.0f) * 100.0f)
        val sign = if (percent >= 0) "+" else ""
        val rateStr = "$sign${"%.0f".format(Locale.US, percent)}%"

        // Minimāls, korekts SSML ar prosody + balss
        return """
            <speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis"
                   xmlns:mstts="https://www.w3.org/2001/mstts" xml:lang="lv-LV">
              <voice name="$voice">
                <prosody rate="$rateStr">${escapeXml(text)}</prosody>
              </voice>
            </speak>
        """.trimIndent()
    }

    private fun escapeXml(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    private fun Float.isOne(): Boolean = kotlin.math.abs(this - 1.0f) < 0.01f
}
