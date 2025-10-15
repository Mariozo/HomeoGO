// ============================================================================
// File: app/src/main/java/lv/mariozo/homeogo/voice/tts/system/SystemTtsEngine.kt
// Module: HomeoGO
// Purpose: Android built-in TextToSpeech engine wrapper (Refactored)
// Created: 27.sep.2025 16:00 (Europe/Riga)
// ver. 2.1
// ============================================================================
package lv.mariozo.homeogo.voice.tts.system

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import lv.mariozo.homeogo.voice.tts.TtsEngine
import java.util.Collections
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalCoroutinesApi::class)
class SystemTtsEngine(context: Context) : TtsEngine { // Removed private val from context

    private var ttsInstanceInternal: TextToSpeech? = null
    private val initializationDeferred = CompletableDeferred<TextToSpeech>()

    private val activeSpeechContinuations =
        Collections.synchronizedMap(mutableMapOf<String, (Result<Unit>) -> Unit>())

    init {
        val listener = TextToSpeech.OnInitListener { status ->
            val currentTts = this.ttsInstanceInternal
            if (status == TextToSpeech.SUCCESS && currentTts != null) {
                try {
                    // Updated Locale constructor
                    currentTts.language = Locale.Builder().setLanguage("lv").setRegion("LV").build()
                    currentTts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            // Optional: Log or handle start of speech
                        }

                        override fun onDone(utteranceId: String?) {
                            activeSpeechContinuations.remove(utteranceId)
                                ?.invoke(Result.success(Unit))
                        }

                        @Deprecated(
                            "Deprecated in Java",
                            ReplaceWith("onError(utteranceId, errorCode)")
                        )
                        override fun onError(utteranceId: String?) {
                            activeSpeechContinuations.remove(utteranceId)
                                ?.invoke(Result.failure(RuntimeException("TTS Error for utteranceId: $utteranceId")))
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            activeSpeechContinuations.remove(utteranceId)
                                ?.invoke(Result.failure(RuntimeException("TTS Error for utteranceId: $utteranceId, errorCode: $errorCode")))
                        }
                    })
                    initializationDeferred.complete(currentTts)
                } catch (e: Exception) {
                    initializationDeferred.completeExceptionally(
                        RuntimeException(
                            "TTS setup failed after initialization: ${e.message}",
                            e
                        )
                    )
                }
            } else {
                initializationDeferred.completeExceptionally(RuntimeException("TTS Initialization Failed with status: $status or TTS instance became null"))
            }
        }
        this.ttsInstanceInternal = TextToSpeech(context, listener)
    }

    override fun name() = "SystemTTS"

    override suspend fun speak(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val tts = initializationDeferred.await()
            val utteranceId = UUID.randomUUID().toString()

            suspendCancellableCoroutine { continuation ->
                activeSpeechContinuations[utteranceId] = { result ->
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                val speakResult = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

                if (speakResult == TextToSpeech.ERROR) {
                    activeSpeechContinuations.remove(utteranceId)
                    if (continuation.isActive) {
                        continuation.resumeWithException(RuntimeException("TTS speak() call returned ERROR immediately."))
                    }
                }

                continuation.invokeOnCancellation {
                    activeSpeechContinuations.remove(utteranceId)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Removed 'override' as TtsEngine interface does not have close()
    fun close() {
        if (initializationDeferred.isCompleted) {
            try {
                // Corrected check for exceptional completion
                if (!initializationDeferred.isCancelled && initializationDeferred.getCompletionExceptionOrNull() == null) {
                    val ttsToShutdown = initializationDeferred.getCompleted()
                    ttsToShutdown.stop()
                    ttsToShutdown.shutdown()
                }
            } catch (_: Exception) { // Changed 'e' to '_' for unused parameter
                // Log or handle exception during shutdown
            }
        } else {
            ttsInstanceInternal?.shutdown()
        }
        ttsInstanceInternal = null
    }
}
