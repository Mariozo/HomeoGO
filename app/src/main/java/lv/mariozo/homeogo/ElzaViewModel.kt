// File: app/src/main/java/lv/mariozo/homeogo/ui/ElzaViewModel.kt
// Project: HomeoGO
// Created: 11.okt.2025 (Rīga)
// ver. 4.0 (FINAL - Clean implementation based on user specification)
// Purpose: ViewModel for ElzaScreen, implementing a robust state machine for voice interaction.
// Features:
//  - Always-on listening for seamless barge-in.
//  - Robust echo cancellation via text normalization.
//  - Temporary "..." speaking bubble that is replaced or removed on completion/interruption.
//  - Toggleable mute mode for text-only responses.
//  - Centralized state management to prevent race conditions and loops.

package lv.mariozo.homeogo.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lv.mariozo.homeogo.BuildConfig
import lv.mariozo.homeogo.logic.ElzaLogicManager
import lv.mariozo.homeogo.voice.SpeechRecognizerManager
import lv.mariozo.homeogo.voice.TtsManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

// #1. ---- Data Models ----------------------------------------------------------

enum class Sender { USER, ELZA }

data class ChatMessage(
    val id: Long,
    val from: Sender,
    val text: String,
    val isSpeaking: Boolean = false, // Used for the temporary "..." bubble
)

data class ElzaScreenState(
    val status: String = "Gatavs",
    val isListening: Boolean = false,
    val isMuted: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val speakingMessage: ChatMessage? = null,
    internal val currentlySpokenText: String? = null, // For echo cancellation
)

// #2. ---- ViewModel ------------------------------------------------------------

class ElzaViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ElzaScreenState())
    val uiState: StateFlow<ElzaScreenState> = _uiState

    private val sttManager: SpeechRecognizerManager
    private val ttsManager: TtsManager
    private val logic: ElzaLogicManager

    private var nextId = 1L

    init {
        sttManager = SpeechRecognizerManager(
            context = application.applicationContext,
            speechKey = BuildConfig.AZURE_SPEECH_KEY,
            speechRegion = BuildConfig.AZURE_SPEECH_REGION,
            language = BuildConfig.STT_LANGUAGE
        )
        ttsManager = TtsManager(
            speechKey = BuildConfig.AZURE_SPEECH_KEY,
            speechRegion = BuildConfig.AZURE_SPEECH_REGION
        )
        logic = ElzaLogicManager(
            ai = { userText, locale -> backendReply(userText, locale) },
            isOnline = { isNetworkAvailable(getApplication()) },
            defaultLocale = BuildConfig.STT_LANGUAGE
        )
    }

    // --- 2.1. Public Actions from UI ---

    fun startListening() {
        // Ja runā → apklusinām, BET NEATGRIEŽAMIES: tūlīt startējam STT
        if (_uiState.value.speakingMessage != null) {
            stopAllActivity(restartListening = false, becauseOfInterrupt = true)
        }
        if (_uiState.value.isListening) return

        _uiState.update { it.copy(status = "Klausos...", isListening = true) }
        sttManager.startListening(object : SpeechRecognizerManager.Callbacks {
            override fun onFinal(text: String) = handleRecognition(text)
            override fun onPartial(text: String) { /* optional */
            }

            override fun onStatus(status: String) {
                _uiState.update { it.copy(status = status) }
            }

            override fun onError(messageLv: String) {
                _uiState.update { it.copy(status = "STT Kļūda: $messageLv", isListening = false) }
            }
        })
    }

    fun stopListening() {
        stopAllActivity(restartListening = false, becauseOfInterrupt = false)
    }

    fun toggleMuteMode() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
        // If Elza was speaking, stop her, as the mode has changed.
        if (_uiState.value.speakingMessage != null) {
            stopAllActivity(restartListening = true, becauseOfInterrupt = true)
        }
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(status = "Vajag mikrofona atļauju!", isListening = false) }
    }

    // --- 2.2. Internal Logic ---

    private fun handleRecognition(recognizedText: String) {
        if (recognizedText.isBlank()) return

        // --- ROBUST ECHO CANCELLATION ---
        val spokenText = _uiState.value.currentlySpokenText
        if (spokenText != null) {
            fun String.normalize() = this.lowercase().filter { it.isLetterOrDigit() }
            val normalizedSpoken = spokenText.normalize()
            val normalizedRecognized = recognizedText.normalize()

            if (normalizedRecognized.isNotEmpty() && (normalizedSpoken.contains(normalizedRecognized) || normalizedRecognized.contains(
                    normalizedSpoken
                ))
            ) {
                Log.d("HomeoGO-Echo", "Ignored echo: '$recognizedText'")
                return // It's an echo, do nothing.
            }
        }

        // --- USER INTERRUPT & PROCESSING ---
        stopAllActivity(restartListening = false, becauseOfInterrupt = true)

        appendMessage(Sender.USER, recognizedText)
        processInput(recognizedText)
    }

    private fun processInput(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = "Domā…") }

            // Create a temporary message that will be shown while Elza thinks and speaks.
            val tempMsg =
                ChatMessage(id = nextId++, from = Sender.ELZA, text = "...", isSpeaking = true)
            _uiState.update { it.copy(speakingMessage = tempMsg) }

            val reply = logic.replyTo(text, locale = BuildConfig.STT_LANGUAGE)

            // If AI fails, remove the temp bubble and go back to listening.
            if (reply.text.isBlank()) {
                _uiState.update { it.copy(status = "AI kļūda", speakingMessage = null) }
                startListening()
                return@launch
            }

            // If muted, just replace the temp bubble with the final text and restart listening.
            if (_uiState.value.isMuted) {
                replaceTempBubbleWithFinal(reply.text, tempMsg.id)
                startListening()
            } else {
                speak(reply.text, tempMsg.id)
            }
        }
    }

    private fun speak(fullText: String, tempMsgId: Long) {
        _uiState.update { it.copy(status = "Runā…", currentlySpokenText = fullText) }

        ttsManager.speak(fullText) { success ->
            val wasInterrupted = _uiState.value.speakingMessage?.id != tempMsgId
            if (wasInterrupted) return@speak

            if (success) {
                replaceTempBubbleWithFinal(fullText, tempMsgId)
            } else {
                // If TTS failed, just clear the temp bubble.
                _uiState.update {
                    it.copy(
                        speakingMessage = null,
                        currentlySpokenText = null,
                        status = "TTS Kļūda"
                    )
                }
            }
            // After speaking (or failing), automatically restart the listening cycle.
            startListening()
        }
    }

    // --- 2.3. State & Message Helpers ---

    private fun stopAllActivity(restartListening: Boolean, becauseOfInterrupt: Boolean) {
        sttManager.stopListening() // Always stop STT
        ttsManager.stop()         // Always stop TTS

        _uiState.update {
            it.copy(
                isListening = false,
                speakingMessage = null, // Always remove the temp bubble
                currentlySpokenText = null,
                status = if (becauseOfInterrupt) "Pārtraukts" else "Apturēts"
            )
        }

        if (restartListening) {
            // Use a small delay to prevent immediate re-triggering issues
            viewModelScope.launch {
                kotlinx.coroutines.delay(100) // Debounce delay
                startListening()
            }
        }
    }

    private fun replaceTempBubbleWithFinal(text: String, tempMsgId: Long) {
        _uiState.update { state ->
            if (state.speakingMessage?.id != tempMsgId) return@update state
            val finalMessage = state.speakingMessage.copy(text = text, isSpeaking = false)
            state.copy(
                messages = state.messages + finalMessage,
                speakingMessage = null,
                currentlySpokenText = null
            )
        }
    }

    private fun appendMessage(from: Sender, text: String) {
        val msg = ChatMessage(id = nextId++, from = from, text = text)
        _uiState.update { it.copy(messages = it.messages + msg) }
    }

    // --- 2.4. Boilerplate ---

    override fun onCleared() {
        super.onCleared()
        sttManager.release()
        ttsManager.release()
    }

    private suspend fun backendReply(userText: String, locale: String): String =
        withContext(Dispatchers.IO) {
            val url = URL(BuildConfig.ELZA_API_BASE.trimEnd('/') + BuildConfig.ELZA_API_PATH)
            (url.openConnection() as HttpURLConnection).run {
                try {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 20_000
                    readTimeout = 20_000
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    if (BuildConfig.ELZA_API_TOKEN.isNotBlank()) {
                        setRequestProperty("Authorization", "Bearer ${BuildConfig.ELZA_API_TOKEN}")
                    }

                    val payload =
                        JSONObject().put("prompt", userText).put("lang", locale).toString()
                    outputStream.use { it.write(payload.toByteArray(StandardCharsets.UTF_8)) }

                    val responseCode = responseCode
                    val responseStream = if (responseCode in 200..299) inputStream else errorStream
                    val rawResponse = responseStream.bufferedReader().use { it.readText() }.trim()

                    if (responseCode !in 200..299) {
                        Log.e("HomeoGO-AI", "Backend Error ($responseCode): $rawResponse")
                        return@withContext ""
                    }

                    JSONObject(rawResponse).optString("reply", "").trim()
                } catch (e: Exception) {
                    Log.e("HomeoGO-AI", "Backend connection failed", e)
                    ""
                } finally {
                    disconnect()
                }
            }
        }

    private fun isNetworkAvailable(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetwork?.let {
            cm.getNetworkCapabilities(it)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } ?: false
    }
}