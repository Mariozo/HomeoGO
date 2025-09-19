
// File: app/src/main/java/lv/mariozo/homeogo/tts/TTSManager.kt
// Module: HomeoGO
// Purpose: Network TTS using Azure Speech (lv-LV-EveritaNeural) + playback via ExoPlayer
// Created: 17.sep.2025 21:15(Europe/Riga)
// ver. 1.0

package lv.mariozo.homeogo.tts

// # --- 1 ------- Imports --------------------------------------------------------
import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.File
import java.util.concurrent.TimeUnit
import lv.mariozo.homeogo.BuildConfig

// # --- 2 ------- Public API -----------------------------------------------------
/**
 * TTSManager:
 *  - Synthesizes Latvian female voice (lv-LV-EveritaNeural) via Azure Speech REST API.
 *  - Produces MP3 and plays it using ExoPlayer.
 *
 * Usage:
 *  val tts = TTSManager(context)
 *  tts.speak("Sveiki, es esmu Elza, un es runāju latviski!")
 *  ...
 *  tts.stop()
 *  tts.release()
 */
class TTSManager(private val appContext: Context) {

    // # --- 2.1 ---- Config ------------------------------------------------------
    data class TtsConfig(
        val region: String = BuildConfig.AZURE_SPEECH_REGION, // e.g., "westeurope", "northeurope"
        val subscriptionKey: String = BuildConfig.AZURE_SPEECH_KEY,
        val voice: String = "lv-LV-EveritaNeural",
        // See: https://learn.microsoft.com/azure/ai-services/speech-service/rest-text-to-speech#audio-outputs
        val outputFormat: String = "audio-16khz-128kbitrate-mono-mp3",
        val speakingRate: Double = 1.0,   // 1.0 = normal
        val speakingPitch: Double = 0.0   // 0.0 = neutral (in semitones)
    )

    private val cfg = TtsConfig()

    // # --- 2.2 ---- HTTP client -------------------------------------------------
    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // # --- 2.3 ---- Player ------------------------------------------------------
    private var player: ExoPlayer? = null

    // # --- 3 ------- Speak (main entry) -----------------------------------------
    /**
     * Synthesizes the given text and plays it. Cancels previous playback if any.
     */
    suspend fun speak(text: String) {
        if (text.isBlank()) return
        stop()
        val audioFile = synthesizeToFile(text)
        playFile(audioFile)
    }

    // # --- 4 ------- Stop / Release ---------------------------------------------
    /**
     * Stops current playback (if playing).
     */
    fun stop() {
        player?.stop()
    }

    /**
     * Releases player resources. Call from onDestroy().
     */
    fun release() {
        player?.release()
        player = null
    }

    // # --- 5 ------- Synthesis ---------------------------------------------------
    private suspend fun synthesizeToFile(text: String): File = withContext(Dispatchers.IO) {
        // Endpoint: https://{region}.tts.speech.microsoft.com/cognitiveservices/v1
        val url = "https://${cfg.region}.tts.speech.microsoft.com/cognitiveservices/v1"

        val ssml = buildSsml(
            text = text,
            voice = cfg.voice,
            rate = cfg.speakingRate,
            pitch = cfg.speakingPitch
        )

        val mediaType = "application/ssml+xml".toMediaType()
        val body = RequestBody.create(mediaType, ssml)

        val req = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Ocp-Apim-Subscription-Key", cfg.subscriptionKey)
            .addHeader("Content-Type", "application/ssml+xml; charset=utf-8")
            .addHeader("X-Microsoft-OutputFormat", cfg.outputFormat)
            .addHeader("User-Agent", "HomeoGO-TTS-Client")
            .build()

        val tmpFile = File.createTempFile("tts_", ".mp3", appContext.cacheDir)

        try {
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val msg = "TTS HTTP ${resp.code}: ${resp.message}"
                    Log.e("TTSManager", msg)
                    throw RuntimeException(msg)
                }
                resp.body?.byteStream()?.use { input ->
                    tmpFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: run {
                    throw RuntimeException("TTS empty body")
                }
            }
        } catch (ce: CancellationException) {
            // Coroutine canceled; clean up
            tmpFile.delete()
            throw ce
        } catch (e: Exception) {
            tmpFile.delete()
            throw e
        }

        tmpFile
    }

    // # --- 6 ------- SSML builder -----------------------------------------------
    /**
     * Builds SSML for Azure TTS. Supports rate (multiplier) and pitch (semitones).
     * Example rate=1.1 -> +10%; pitch=+2.0 -> up 2 semitones; pitch=-2.0 -> down 2 semitones.
     */
    private fun buildSsml(text: String, voice: String, rate: Double, pitch: Double): String {
        val ratePct = ((rate - 1.0) * 100.0).toInt()
        val rateStr = if (ratePct >= 0) "+${ratePct}%" else "${ratePct}%"

        // Pitch in semitones (st)
        val pitchStr = when {
            pitch > 0.0 -> "+${pitch}st"
            pitch < 0.0 -> "${pitch}st"
            else -> "0st"
        }

        val safeText = text.xmlEscape()

        return """
            <speak version="1.0" xml:lang="lv-LV" xmlns="http://www.w3.org/2001/10/synthesis" xmlns:mstts="https://www.w3.org/2001/mstts">
              <voice name="$voice">
                <prosody rate="$rateStr" pitch="$pitchStr">
                  $safeText
                </prosody>
              </voice>
            </speak>
        """.trimIndent()
    }

    // # --- 7 ------- Playback ----------------------------------------------------
    private fun playFile(file: File) {
        if (player == null) {
            player = ExoPlayer.Builder(appContext).build()
        }
        val uri = Uri.fromFile(file)
        val item = MediaItem.fromUri(uri)
        player!!.setMediaItem(item)
        player!!.prepare()
        player!!.play()
    }
}

// # --- 8 ------- Helpers --------------------------------------------------------
private fun String.xmlEscape(): String =
    this.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
