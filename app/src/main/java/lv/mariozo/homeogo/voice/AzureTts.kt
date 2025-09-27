// ============================================================================
// File: app/src/main/java/lv/mariozo/homeogo/voice/AzureTts.kt
// Module: HomeoGO
// Purpose: Azure Cognitive Services TTS (Everita) – stable synth with diagnostics
// Created: 25.sep.2025 21:15 (Europe/Riga)
// ver. 2.1
// Author: Māris + ChatGPT
// Notes:
//   - Requires dependency: com.microsoft.cognitiveservices.speech:client-sdk:1.46.0
//   - INTERNET permission must be present in AndroidManifest.xml
//   - Region must be short name like "northeurope" (NOT "North Europe", NOT URL)
//   - Endpoint is NOT needed for Java/Kotlin SDK.
//   - Uses coroutines: synthesis runs on Dispatchers.IO, callback returns on Main.
// ============================================================================

package lv.mariozo.homeogo.voice

import android.content.Context
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
# --- 1 -----------------------------------------------------------------------
# Public API
# -----------------------------------------------------------------------------
*/
object AzureTts {

    // Voice constants (Latvian voices)
    private const val LANG = "lv-LV"
    private const val VOICE = "lv-LV-EveritaNeural"

    /**
     * Speak the given [text] using Azure Speech.
     *
     * @param context Android context for filesDir (SDK log file)
     * @param text Text to synthesize
     * @param key Azure Speech resource key (Key1 or Key2)
     * @param region Azure region short name, e.g. "northeurope"
     * @param onResult Callback with human-friendly status string
     */
    fun speak(
        context: Context,
        text: String,
        key: String,
        region: String,
        onResult: (String) -> Unit,
    ) {
        // <-- ŠEIT ieliec pārbaudi
        if (region.isBlank() || region.equals("null", ignoreCase = true)) {
            onResult("CONFIG ERROR: Azure region is '$region'. Ieliec 'northeurope' BuildConfig/gradle.properties.")
            return
        }

        val speechConfig: SpeechConfig? =
            try { // Ensure speechConfig is accessible in the coroutine
                SpeechConfig.fromSubscription(key, region).apply {
                    speechSynthesisLanguage = LANG
                    speechSynthesisVoiceName = VOICE
                    setProperty(
                        "Speech_LogFilename",
                        "${context.filesDir.path}/azure_speech_sdk.log"
                    )
                }
            } catch (e: Exception) {
                onResult("INIT ERROR: ${e.message ?: e::class.java.simpleName}")
                return
            }

        CoroutineScope(Dispatchers.IO).launch {
            val status = try {
                if (speechConfig == null) { // Check if speechConfig initialization failed
                    "INIT ERROR: speechConfig is null"
                } else {
                    // CORRECT: run sync call on IO thread; auto-close resources
                    val result = AudioConfig.fromDefaultSpeakerOutput().use { audio ->
                        SpeechSynthesizer(speechConfig, audio).use { synth ->
                            synth.SpeakText(text)   // ← paliek sinhroni, jo jau esam Dispatchers.IO
                        }
                    }

                    when (result.reason) {
                        ResultReason.SynthesizingAudioCompleted -> "OK"
                        ResultReason.Canceled -> {
                            val c = SpeechSynthesisCancellationDetails.fromResult(result)
                            "CANCELED: reason=${c.reason}; code=${c.errorCode}; details=${c.errorDetails?.trim()}"
                        }

                        else -> "UNEXPECTED: ${result.reason}"
                    }
                }
            } catch (e: Exception) {
                "CALL ERROR: ${e.message ?: e::class.java.simpleName}"
            }

            withContext(Dispatchers.Main) {
                onResult(status)
            }
        }
    } // End of speak function
} // End of AzureTts object

/*
# --- 2 -----------------------------------------------------------------------
# Usage sketch (for your UI / ViewModel)
# -----------------------------------------------------------------------------
# val key = settings.azureKey          // e.g., read from Settings screen / secure storage
# val region = "northeurope"           // EXACT region short name
# AzureTts.speak(context, "Sveiki! Te runā Everita.", key, region) { msg ->
#     // Show Toast/snackbar/log with 'msg'
# }
# -----------------------------------------------------------------------------
*/
