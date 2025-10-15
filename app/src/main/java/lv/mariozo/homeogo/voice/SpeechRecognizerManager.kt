// File: app/src/main/java/lv/mariozo/homeogo/voice/SpeechRecognizerManager.kt
// Project: HomeoGO
// Created: 03.okt.2025 13:45 (Rīga)
// ver. 1.3
// Purpose: Azure Speech SDK Speech-to-Text manager ar drošu start/stop, detalizētu
//          diagnostiku (Logcat) un emulatoram draudzīgiem laikaouts.
// Comments:
//  - Atslēga/reģions tiek padoti konstruktorā (nav tiešas atkarības no BuildConfig),
//    bet valodu ņemam no BuildConfig.STT_LANGUAGE, ko vari nomainīt (piem., "en-US").
//  - stopListening() tagad reāli aptur atpazīšanu: gaida stopContinuousRecognitionAsync().get()
//    un tikai tad aizver resursus. Tas novērš “recognized: ''” pēc Stop.
//  - Emulatoriem palielināts Initial/End silence timeouts (7s/1.5s).
//  - Logcat tag: "HomeoGO-STT"

package lv.mariozo.homeogo.voice

// 1. ---- Imports ---------------------------------------------------------------
import android.content.Context
import android.util.Log
import com.microsoft.cognitiveservices.speech.CancellationDetails
import com.microsoft.cognitiveservices.speech.CancellationReason
import com.microsoft.cognitiveservices.speech.PropertyId
import com.microsoft.cognitiveservices.speech.SessionEventArgs
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognitionCanceledEventArgs
import com.microsoft.cognitiveservices.speech.SpeechRecognitionEventArgs
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lv.mariozo.homeogo.BuildConfig
import java.util.concurrent.TimeUnit

// 2. ---- Manager ---------------------------------------------------------------
class SpeechRecognizerManager(
    private val context: Context,
    speechKey: String,
    speechRegion: String,
    language: String = BuildConfig.STT_LANGUAGE, // piemēram "lv-LV" vai "en-US"
) {

    private val tag = "HomeoGO-STT"

    // 2.1 ---- SpeechConfig ar valodu + silence timeouts -----------------------
    private val speechConfig: SpeechConfig =
        SpeechConfig.fromSubscription(speechKey, speechRegion).apply {
            speechRecognitionLanguage = language
            // Palielinām klusuma toleranci (emulatoram noder)
            setProperty(PropertyId.SpeechServiceConnection_InitialSilenceTimeoutMs, "7000")
            setProperty(PropertyId.SpeechServiceConnection_EndSilenceTimeoutMs, "1500")
        }

    private var audioConfig: AudioConfig? = null
    private var recognizer: SpeechRecognizer? = null

    interface Callbacks {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onStatus(status: String)
        fun onError(messageLv: String)
    }

    // 3. ---- Start -------------------------------------------------------------
    fun startListening(callbacks: Callbacks) {
        // Vienmēr notīrām iepriekšējo sesiju
        stopListening()

        callbacks.onStatus("Inicializē mikrofona ieeju…")
        Log.d(tag, "startListening(): creating AudioConfig.fromDefaultMicrophoneInput()")

        audioConfig = AudioConfig.fromDefaultMicrophoneInput()
        recognizer = SpeechRecognizer(speechConfig, audioConfig).apply {
            // Partial results
            recognizing.addEventListener { _: Any?, e: SpeechRecognitionEventArgs ->
                val txt = e.result?.text.orEmpty()
                CoroutineScope(Dispatchers.Main).launch {
                    if (txt.isNotBlank()) callbacks.onPartial(txt)
                }
                Log.d(tag, "recognizing: '${e.result?.text}' (reason=${e.result?.reason})")
            }

            // Final results
            recognized.addEventListener { _: Any?, e: SpeechRecognitionEventArgs ->
                val finalText = e.result?.text.orEmpty()
                CoroutineScope(Dispatchers.Main).launch {
                    callbacks.onFinal(finalText)
                    callbacks.onStatus(if (finalText.isBlank()) "Gala rezultāts tukšs (klusums?)" else "Atpazīts.")
                }
                Log.d(tag, "recognized: '${e.result?.text}' (reason=${e.result?.reason})")
            }

            // Cancellation / errors
            canceled.addEventListener { _: Any?, e: SpeechRecognitionCanceledEventArgs ->
                val details: CancellationDetails = CancellationDetails.fromResult(e.result)
                val msg = when (details.reason) {
                    CancellationReason.Error -> "STT atcelts: kļūda (${details.errorCode}): ${details.errorDetails}"
                    CancellationReason.EndOfStream -> "STT atcelts: datu beigas"
                    CancellationReason.CancelledByUser -> "STT atcelts: lietotājs"
                    else -> "STT atcelts: ${details.reason}"
                }
                Log.w(
                    tag,
                    "canceled: reason=${details.reason} code=${details.errorCode} details=${details.errorDetails}"
                )
                CoroutineScope(Dispatchers.Main).launch { callbacks.onError(msg) }
            }

            // Session lifecycle
            sessionStarted.addEventListener { _: Any?, args: SessionEventArgs ->
                Log.d(tag, "sessionStarted: ${args.sessionId}")
                CoroutineScope(Dispatchers.Main).launch {
                    callbacks.onStatus("Sesija sākta (mic OK). Runā tagad.")
                }
            }
            sessionStopped.addEventListener { _: Any?, args: SessionEventArgs ->
                Log.d(tag, "sessionStopped: ${args.sessionId}")
                CoroutineScope(Dispatchers.Main).launch {
                    callbacks.onStatus("Sesija apturēta.")
                }
            }
        }

        Log.d(tag, "startContinuousRecognitionAsync()")
        recognizer?.startContinuousRecognitionAsync()
    }

    // 4. ---- Stop (drošs) ------------------------------------------------------
    fun stopListening() {
        Log.d(tag, "stopListening() called")
        recognizer?.let { rec ->
            // Gaida, līdz Azure apturēs continuous recognition (max 2s), tad aizver
            runCatching {
                rec.stopContinuousRecognitionAsync().get(2, TimeUnit.SECONDS)
            }.onFailure { e ->
                Log.w(tag, "stopContinuousRecognitionAsync error: $e")
            }
            runCatching { rec.close() }
        }
        recognizer = null

        runCatching { audioConfig?.close() }
        audioConfig = null

        Log.d(tag, "Recognizer & AudioConfig closed")
    }

    // 5. ---- Release -----------------------------------------------------------
    fun release() {
        Log.d(tag, "release()")
        stopListening()
        runCatching { speechConfig.close() }
    }
}
