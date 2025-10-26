// File: app/src/main/java/lv/mariozo/homeogo/ElzaViewModel.kt
// Project: HomeoGO
// Created: 15.okt.2025 - 11:30 (Europe/Riga)
// ver. 6.1 (SPS-38: Internationalized all status strings)
// Purpose: ViewModel for ElzaScreen, using string resources for all user-facing text.
// Author: Gemini Agent (Burtnieks & Elza Assistant)

package lv.mariozo.homeogo

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lv.mariozo.homeogo.ui.ChatMessage
import lv.mariozo.homeogo.ui.ElzaScreenState
import lv.mariozo.homeogo.ui.InteractionMode
import lv.mariozo.homeogo.ui.Sender
import lv.mariozo.homeogo.voice.SpeechRecognizerManager
import lv.mariozo.homeogo.voice.TtsManager
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import android.util.Log as ALog

class ElzaViewModel(app: Application) : AndroidViewModel(app) {

    // STT / TTS
    private val sttManager = SpeechRecognizerManager(
        context = app,
        speechKey = BuildConfig.AZURE_SPEECH_KEY,
        speechRegion = BuildConfig.AZURE_SPEECH_REGION,
        language = BuildConfig.STT_LANGUAGE
    )
    private val ttsManager = TtsManager(
        speechKey = BuildConfig.AZURE_SPEECH_KEY,
        speechRegion = BuildConfig.AZURE_SPEECH_REGION,
        voiceName = "lv-LV-EveritaNeural"
    )

    // UI State
    private val _uiState = MutableStateFlow(
        ElzaScreenState(
            status = app.getString(R.string.status_ready),
            isListening = false,
            messages = emptyList(),
            speakingMessage = null
        )
    )
    val uiState: StateFlow<ElzaScreenState> = _uiState

    // Vienkāršais režīms: TEXT (tikai burbulis) / VOICE (burbulis + TTS)
    fun setInteractionMode(mode: InteractionMode) {
        _uiState.update { it.copy(interactionMode = mode) }
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(status = getString(R.string.status_mic_permission_needed)) }
    }

    // STT control
    fun startListening() {
        if (_uiState.value.isListening) return

        stopSpeaking() // drošībai
        _uiState.update {
            it.copy(
                status = getString(R.string.status_listening),
                isListening = true
            )
        }

        sttManager.startListening(object : SpeechRecognizerManager.Callbacks {
            override fun onPartial(text: String) {
                if (text.isNotBlank()) {
                    _uiState.update { it.copy(status = getString(R.string.status_hearing, text)) }
                }
            }

            override fun onFinal(text: String) {
                if (!_uiState.value.isListening) return

                if (text.isBlank()) {
                    _uiState.update { it.copy(status = getString(R.string.status_listening)) }
                    return
                }

                stopListening()
                appendMessage(Sender.USER, text)
                processInput(text)
            }

            override fun onStatus(status: String) {
                _uiState.update { it.copy(status = status) }
            }

            override fun onError(messageLv: String) {
                _uiState.update {
                    it.copy(
                        status = getString(R.string.status_stt_error, messageLv),
                        isListening = false
                    )
                }
            }
        })
    }

    fun stopListening() {
        sttManager.stopListening()
        _uiState.update { it.copy(isListening = false) }
    }

    fun stopSpeaking() {
        ttsManager.stop()
        if (_uiState.value.speakingMessage != null) {
            _uiState.update { it.copy(speakingMessage = null) }
        }
    }

    // User text → backend → reply → speak if VOICE
    private fun processInput(userText: String) {
        if (!isNetworkAvailable(getApplication())) {
            appendMessage(Sender.ELZA, getString(R.string.status_no_network))
            return
        }

        showSpeakingBubble()

        viewModelScope.launch(Dispatchers.IO) {
            val reply = backendReply(userText, lang = "lv")
            launch(Dispatchers.Main) {
                replaceSpeakingWithFinal(reply.ifBlank { "…" })

                val speakAllowed = _uiState.value.interactionMode == InteractionMode.VOICE
                if (speakAllowed) speak(reply)
                else _uiState.update { it.copy(status = getString(R.string.status_ready)) } // TEXT režīms — paliekam gaidīšanas stāvoklī
            }
        }
    }

    private fun speak(fullText: String) {
        _uiState.update { it.copy(status = getString(R.string.status_speaking)) }
        ttsManager.speak(fullText) {
            _uiState.update { it.copy(status = getString(R.string.status_ready)) }
        }
    }

    // UI helperi
    private fun appendMessage(from: Sender, text: String) {
        _uiState.update { st ->
            st.copy(messages = st.messages + ChatMessage(System.nanoTime(), from, text))
        }
    }

    private fun showSpeakingBubble() {
        _uiState.update { st ->
            st.copy(
                speakingMessage = ChatMessage(
                    id = System.nanoTime(),
                    from = Sender.ELZA,
                    text = "...",
                    isSpeaking = true
                )
            )
        }
    }

    private fun replaceSpeakingWithFinal(finalText: String) {
        _uiState.update { st ->
            st.copy(
                speakingMessage = null,
                messages = st.messages + ChatMessage(System.nanoTime(), Sender.ELZA, finalText)
            )
        }
    }

    // Backend
    private fun backendReply(prompt: String, lang: String): String {
        return runCatching {
            val base = BuildConfig.ELZA_API_BASE.trimEnd('/')
            val path = BuildConfig.ELZA_API_PATH.ifBlank { "/elza/reply" }
            val url = URL("$base$path")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                val tok = BuildConfig.ELZA_API_TOKEN
                if (tok.isNotBlank()) setRequestProperty("Authorization", "Bearer $tok")
            }
            val payload = JSONObject().put("prompt", prompt).put("lang", lang).toString()
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(payload) }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            conn.disconnect()
            if (code !in 200..299) {
                ALog.e("HomeoGO-API", "HTTP $code: $body")
                return@runCatching getString(R.string.status_backend_http_error, code)
            }
            val trimmed = body.trim()
            if (trimmed.startsWith("{")) JSONObject(trimmed).optString(
                "reply",
                trimmed
            ) else trimmed
        }.getOrElse { e ->
            ALog.e("HomeoGO-API", "backendReply exception", e)
            getString(R.string.status_backend_connection_error)
        }
    }

    // Helper to get string resources
    private fun getString(resId: Int, vararg formatArgs: Any): String {
        return getApplication<Application>().getString(resId, *formatArgs)
    }

    // Network
    private fun isNetworkAvailable(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val n = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(n) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
        sttManager.stopListening()
    }
}
