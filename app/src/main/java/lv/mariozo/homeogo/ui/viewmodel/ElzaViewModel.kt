// File: app/src/main/java/lv/mariozo/homeogo/ui/viewmodel/ElzaViewModel.kt
// Project: HomeoGO
// Created: 26.Oct.2025 10:55 (Europe/Riga)
// ver. 1.0 (RESTORE minimal): use SpeechRecognizerManager (StateFlow) + TTSManager (system TTS)
// Purpose: ViewModel for Elza screen (STT->process->TTS pipeline without Azure deps)

package lv.mariozo.homeogo.ui.viewmodel

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
import kotlinx.coroutines.withContext
import lv.mariozo.homeogo.speech.SpeechRecognizerManager
import lv.mariozo.homeogo.voice.TTSManager

// Minimal UI state for restore path
data class ElzaUiState(
    val status: String = "Idle",
    val recognizedText: String = "",
    val isListening: Boolean = false,
    val speaking: Boolean = false
)

class ElzaViewModel(app: Application) : AndroidViewModel(app) {

    private val _ui = MutableStateFlow(ElzaUiState())
    val ui: StateFlow<ElzaUiState> = _ui

    // STT: Android SpeechRecognizer wrapper (StateFlow API)
    private val stt = SpeechRecognizerManager(
        context = app,
        scope = viewModelScope
    )

    // TTS: system TextToSpeech (prefers Google TTS if present)
    private val tts = TTSManager(
        context = app,
        preferredEnginePackage = "com.google.android.tts"
    )

    init {
        observeSttState()
    }

    // --- STT control ---
    fun startListening() {
        _ui.update { it.copy(status = "Listening…", isListening = true) }
        stt.startListening()
    }

    fun stopListening() {
        stt.stopListening()
        _ui.update { it.copy(isListening = false, status = "Idle") }
    }

    // --- Speak control ---
    fun stopSpeaking() {
        // Minimal TTSManager has no stop(), only release(); here we simply mark UI as not speaking.
        _ui.update { it.copy(speaking = false, status = "Idle") }
    }

    // --- Observe STT state flow ---
    private fun observeSttState() {
        viewModelScope.launch(Dispatchers.Main) {
            stt.state.collect { s ->
                when (s) {
                    is SpeechRecognizerManager.SttState.Idle -> {
                        _ui.update { it.copy(isListening = false, status = "Idle") }
                    }

                    is SpeechRecognizerManager.SttState.Listening -> {
                        _ui.update { it.copy(isListening = true, status = "Listening…") }
                    }

                    is SpeechRecognizerManager.SttState.Partial -> {
                        _ui.update {
                            it.copy(
                                isListening = true,
                                recognizedText = s.text,
                                status = "Partial…"
                            )
                        }
                    }

                    is SpeechRecognizerManager.SttState.Final -> {
                        val text = s.text.trim()
                        _ui.update {
                            it.copy(
                                isListening = false,
                                recognizedText = text,
                                status = "Final ✓"
                            )
                        }
                        if (text.isNotEmpty()) processInput(text)
                    }

                    is SpeechRecognizerManager.SttState.Error -> {
                        _ui.update { it.copy(isListening = false, status = "Error: ${s.message}") }
                    }
                }
            }
        }
    }

    // --- Pipeline: user text -> (optionally backend) -> TTS ---
    private fun processInput(userText: String) {
        // If you have a backend, enable the network branch; otherwise speak locally
        viewModelScope.launch(Dispatchers.IO) {
            val reply = runCatching {
                // Placeholder: return input as echo for now
                // Replace with backend call if needed.
                userText
            }.getOrElse { "…" }

            withContext(Dispatchers.Main) {
                speak(reply)
            }
        }
    }

    private fun speak(text: String) {
        if (text.isBlank()) {
            _ui.update { it.copy(status = "Nothing to speak") }
            return
        }
        _ui.update { it.copy(speaking = true, status = "Speaking…") }
        tts.speak(text) // fire-and-forget
        _ui.update { it.copy(speaking = false, status = "Idle") }
    }

    // --- Net helper (if you later enable backend HTTP) ---
    private fun isNetworkAvailable(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val n = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(n) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onCleared() {
        super.onCleared()
        // Release TTS resources; reset STT
        tts.release()
        stt.destroyRecognizer()
    }
}
