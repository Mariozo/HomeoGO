// File: app/src/main/java/lv/mariozo/homeogo/ui/ElzaViewModel.kt
// Project: HomeoGO
// Created: 04.okt.2025 14:25 (Rīga)
// ver. 1.5
// Purpose: ViewModel for ElzaScreen — STT/TTS, chat history, AI-first logic with clear diagnostics.
// Comments:
//  - Reads AI endpoint config from BuildConfig (ELZA_API_BASE/ELZA_API_PATH/ELZA_API_TOKEN).
//  - Shows precise status on network/HTTP/JSON errors so you can pinpoint why AI reply is empty.

package lv.mariozo.homeogo.ui

// 1. ---- Imports ---------------------------------------------------------------
import lv.mariozo.homeogo.BuildConfig as AppBuildConfig

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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lv.mariozo.homeogo.logic.ElzaLogicManager
import lv.mariozo.homeogo.logic.ElzaResponse
import lv.mariozo.homeogo.voice.SpeechRecognizerManager
import lv.mariozo.homeogo.voice.TtsManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets

// 2. ---- Chat model ------------------------------------------------------------
enum class Sender { USER, ELZA }

data class ChatMessage(
    val id: Long,
    val from: Sender,
    val text: String,
)

// 3. ---- UI State --------------------------------------------------------------
data class ElzaScreenState(
    val status: String = "Idle",
    val recognizedText: String = "",
    val isListening: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
)

// 4. ---- ViewModel -------------------------------------------------------------
class ElzaViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ElzaScreenState())
    val uiState: StateFlow<ElzaScreenState> = _uiState

    private val sttManager: SpeechRecognizerManager = SpeechRecognizerManager(
        context = application.applicationContext,
        speechKey = AppBuildConfig.AZURE_SPEECH_KEY,
        speechRegion = AppBuildConfig.AZURE_SPEECH_REGION,
        language = AppBuildConfig.STT_LANGUAGE
    )

    private val ttsManager: TtsManager = TtsManager(
        speechKey = AppBuildConfig.AZURE_SPEECH_KEY,
        speechRegion = AppBuildConfig.AZURE_SPEECH_REGION
    )

    // 4.1 ---- AI-first logic manager (uses backendReply + isOnline) -----------
    private val logic = ElzaLogicManager(
        ai = { userText, locale -> backendReply(userText, locale) },
        isOnline = { isNetworkAvailable(getApplication()) },
        defaultLocale = AppBuildConfig.STT_LANGUAGE
    )

    private var nextId: Long = 1L
    private fun newId(): Long = nextId++

    // STT ↔ TTS echo guard
    private var isSpeaking: Boolean = false

    // 4.2 ---- STT control ------------------------------------------------------
    fun startListening() {
        if (isSpeaking) {
            _uiState.value = _uiState.value.copy(status = "Pagaidi — runāju…")
            return
        }
        _uiState.value = _uiState.value.copy(status = "Klausos...", isListening = true)
        sttManager.startListening(object : SpeechRecognizerManager.Callbacks {
            override fun onPartial(text: String) {
                if (isSpeaking) return
                _uiState.value =
                    _uiState.value.copy(status = "Dzird daļu...", recognizedText = text)
            }

            override fun onFinal(text: String) {
                if (isSpeaking) return
                _uiState.value = _uiState.value.copy(
                    status = "Atpazīts!",
                    recognizedText = text,
                    isListening = false
                )
                if (text.isNotBlank()) {
                    appendMessage(Sender.USER, text)
                    processInput(text)
                }
            }

            override fun onStatus(status: String) {
                _uiState.value = _uiState.value.copy(status = status)
            }

            override fun onError(messageLv: String) {
                _uiState.value =
                    _uiState.value.copy(status = "Kļūda: $messageLv", isListening = false)
            }
        })
    }

    fun stopListening() {
        sttManager.stopListening()
        _uiState.value = _uiState.value.copy(status = "Apturēts", isListening = false)
    }

    fun onPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            status = "Mikrofona atļauja ir nepieciešama!",
            isListening = false
        )
    }

    // 4.3 ---- TTS control ------------------------------------------------------
    fun speakTest(text: String) {
        if (_uiState.value.isListening) stopListening()
        isSpeaking = true
        _uiState.value = _uiState.value.copy(status = "Runā: $text")
        ttsManager.speak(text) { ok ->
            isSpeaking = false
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    status = if (ok) "Atskaņošana pabeigta" else "TTS kļūda"
                )
            }
        }
    }

    // 4.4 ---- Logic bridge (STT → AI → TTS) -----------------------------------
    private fun processInput(text: String) {
        viewModelScope.launch {
            // Pirms izsaukuma — skaidrs statuss, kas notiek
            _uiState.value = _uiState.value.copy(status = "Domāju…")
            val reply: ElzaResponse = logic.replyTo(text, locale = AppBuildConfig.STT_LANGUAGE)

            if (reply.text.isBlank()) {
                _uiState.value =
                    _uiState.value.copy(status = "Nevaru atbildēt šobrīd (tukša atbilde).")
                return@launch
            }

            appendMessage(Sender.ELZA, reply.text)
            speakTest(reply.text)
        }
    }

    // 4.5 ---- Chat helpers -----------------------------------------------------
    private fun appendMessage(from: Sender, text: String) {
        val msg = ChatMessage(id = newId(), from = from, text = text)
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + msg)
    }

    fun clearChat() {
        _uiState.value =
            _uiState.value.copy(messages = emptyList(), recognizedText = "", status = "Notīrīts")
    }

    // 4.6 ---- Cleanup -----------------------------------------------------------
    override fun onCleared() {
        super.onCleared()
        sttManager.release()
        ttsManager.release()
    }

    // 5. ---- Backend (AI) adapter ----------------------------------------------
    private companion object {
        private const val TAG = "HomeoGO-AI"
        private const val TIMEOUT_MS = 15_000
    }

    /**
     * POST {text, locale} → {"reply": "..."} (vai {"answer": "..."})
     * Precīzi kļūdu paziņojumi atgriežas UI statusā.
     */
    private suspend fun backendReply(userText: String, locale: String): String =
        withContext(Dispatchers.IO) {
            val base = AppBuildConfig.ELZA_API_BASE
            val path = AppBuildConfig.ELZA_API_PATH
            val token = AppBuildConfig.ELZA_API_TOKEN

            if (base.isBlank()) {
                Log.w(TAG, "ELZA_API_BASE nav konfigurēts")
                // Atgriežam tukšu, lai LogicManager parādītu godīgu fallback
                return@withContext ""
            }

            val url = URL(base.trimEnd('/') + path)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (token.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            val payload = JSONObject()
                .put("text", userText)
                .put("locale", locale)
                .toString()
                .toByteArray(StandardCharsets.UTF_8)

            try {
                conn.outputStream.use { it.write(payload) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val raw = stream.bufferedReader().use { it.readText() }.trim()

                if (code !in 200..299) {
                    Log.w(TAG, "HTTP $code: $raw")
                    // Padodam tukšu → LogicManager sniegs skaidru fallback
                    return@withContext ""
                }

                // Izvelkam "reply" vai "answer"
                val json = runCatching { JSONObject(raw) }.getOrNull()
                val reply = when {
                    json == null -> ""
                    json.has("reply") -> json.optString("reply")
                    json.has("answer") -> json.optString("answer")
                    else -> ""
                }.trim()

                if (reply.isBlank()) {
                    Log.w(TAG, "AI atbilde tukša vai nezināms formāts: $raw")
                }
                reply
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Timeout ($TIMEOUT_MS ms) pie $url", e)
                ""
            } catch (e: Exception) {
                Log.e(TAG, "Kļūda, izsaucot $url", e)
                ""
            } finally {
                conn.disconnect()
            }
        }

    // 6. ---- Connectivity helper -----------------------------------------------
    private fun isNetworkAvailable(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
