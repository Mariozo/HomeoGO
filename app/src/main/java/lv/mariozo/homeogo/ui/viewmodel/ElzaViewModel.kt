// File: app/src/main/java/lv/mariozo/homeogo/ui/viewmodel/ElzaViewModel.kt
// Module: HomeoGO
// Purpose: ViewModel for ElzaScreen (manages UI state, connects STT/TTS managers)
// Created: 21.sep.2025 15:35
// ver. 1.0


package lv.mariozo.homeogo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import lv.mariozo.homeogo.speech.SpeechRecognizerManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import lv.mariozo.homeogo.BuildConfig
import lv.mariozo.homeogo.voice.TtsRouter
import lv.mariozo.homeogo.voice.tts.system.SystemTtsEngine
import lv.mariozo.homeogo.voice.tts.azure.AzureTtsEngine


data class ElzaUiState(
    val status: String = "Idle",
    val recognizedText: String = "",
    val isListening: Boolean = false,
)

class ElzaViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(ElzaUiState())
    val uiState = _uiState.asStateFlow()

    private val ttsRouter = TtsRouter(
        engines = listOf(
            SystemTtsEngine(app),
            AzureTtsEngine(
                context = app,
                key = BuildConfig.AZURE_SPEECH_KEY,
                region = BuildConfig.AZURE_SPEECH_REGION
            )
        ),
        preferred = "AzureTTS" // vai "SystemTTS", ja gribi sistēmas balsi pēc noklusējuma
    )

    private val srm = SpeechRecognizerManager(context = app, scope = viewModelScope)

    init {
        viewModelScope.launch {
            srm.state.collect { srmState ->
                when (srmState) {
                    is SpeechRecognizerManager.SttState.Idle -> {
                        _uiState.value = _uiState.value.copy(
                            status = "Idle",
                            // Optionally clear recognizedText: recognizedText = "",
                            isListening = false
                        )
                    }

                    is SpeechRecognizerManager.SttState.Listening -> {
                        _uiState.value = _uiState.value.copy(
                            status = "Listening...",
                            isListening = true
                        )
                    }

                    is SpeechRecognizerManager.SttState.Partial -> {
                        _uiState.value = _uiState.value.copy(
                            recognizedText = srmState.text,
                            status = "Partial...",
                            isListening = true
                        )
                    }

                    is SpeechRecognizerManager.SttState.Final -> {
                        _uiState.value = _uiState.value.copy(
                            recognizedText = srmState.text,
                            status = if (srmState.text.isNotEmpty()) "Final ✓" else "Nothing recognized",
                            isListening = false
                        )
                    }

                    is SpeechRecognizerManager.SttState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            status = "Error: ${srmState.message}",
                            recognizedText = _uiState.value.recognizedText, // Keep existing text on error
                            isListening = false
                        )
                    }
                }
            }
        }
    }

    fun startListening() {
        srm.startListening()
        // Update UI optimistically, srm.state will provide the authoritative state shortly
        _uiState.value =
            _uiState.value.copy(recognizedText = "", status = "Initializing...", isListening = true)
    }

    fun stopListening() {
        srm.stopListening()
        // UI state will be updated via srm.state flow (e.g., to Idle or Error)
    }

    fun speak(text: String) {
        if (text.isNotBlank()) {
            tts.speak(text)
            _uiState.value = _uiState.value.copy(status = "Speaking...")
        } else {
            _uiState.value = _uiState.value.copy(status = "Nothing to speak")
        }
    }

    fun reportPermissionDenied() {
        _uiState.value =
            _uiState.value.copy(status = "Microphone permission denied", isListening = false)
    }

    override fun onCleared() {
        super.onCleared()
        tts.release()
        srm.destroyRecognizer()
    }
}
