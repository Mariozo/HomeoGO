// File: app/src/main/java/lv/mariozo/homeogo/ui/viewmodel/ElzaViewModel.kt
// Project: HomeoGO
// Created: 26.Oct.2025 10:55 (Europe/Riga)
// ver. 1.0 (RESTORE minimal): use SpeechRecognizerManager (StateFlow) + TTSManager (system TTS)
// Purpose: ViewModel for Elza screen (STT->process->TTS pipeline without Azure deps)

package lv.mariozo.homeogo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lv.mariozo.homeogo.voice.SpeechRecognizerManager

data class ElzaUiState(
    val status: String = "Idle",
    val isListening: Boolean = false,
    val speaking: Boolean = false,
)

class ElzaViewModel(app: Application) : AndroidViewModel(app) {

    private val stt = SpeechRecognizerManager(app)

    private val _ui = MutableStateFlow(ElzaUiState())
    val ui: StateFlow<ElzaUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            stt.state.collect { s ->
                when (s) {
                    is SpeechRecognizerManager.SttState.Idle -> {
                        _ui.update { it.copy(isListening = false, status = "Idle") }
                    }

                    is SpeechRecognizerManager.SttState.Listening -> {
                        _ui.update { it.copy(isListening = true, status = "Klausos…") }
                    }

                    is SpeechRecognizerManager.SttState.Partial -> {
                        _ui.update { it.copy(status = s.text) }
                    }

                    is SpeechRecognizerManager.SttState.Final -> {
                        _ui.update { it.copy(isListening = false, status = s.text) }
                    }

                    is SpeechRecognizerManager.SttState.Error -> {
                        _ui.update { it.copy(isListening = false, status = "Kļūda: ${s.message}") }
                    }
                }
            }
        }
    }

    fun startListening() {
        _ui.update { it.copy(isListening = true, status = "Klausos…") }
        stt.startListening()
    }

    fun stopListening() {
        stt.stopListening()
        _ui.update { it.copy(isListening = false, status = "Idle") }
    }

    fun stopSpeaking() {
        _ui.update { it.copy(speaking = false, status = "Idle") }
    }
}
