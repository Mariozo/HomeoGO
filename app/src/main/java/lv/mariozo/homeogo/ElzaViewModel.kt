// File: app/src/main/java/lv/mariozo/homeogo/ui/ElzaViewModel.kt
// Project: HomeoGO (Android, Jetpack Compose + Material3)
// Created: 03.okt.2025 07:50 (Rīga)
// ver. 1.1
// Purpose: ViewModel providing ElzaScreenState, wiring Azure STT/TTS managers
//          and exposing startListening, stopListening, speakTest callbacks.
// Comments:
//  - Holds StateFlow<ElzaScreenState> for Compose.
//  - Wraps SpeechRecognizerManager (STT) and TtsManager (TTS).
//  - Uses coroutineScope for background operations.

package lv.mariozo.homeogo.ui

// 1. ---- Imports ---------------------------------------------------------------
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import lv.mariozo.homeogo.voice.SpeechRecognizerManager
import lv.mariozo.homeogo.voice.TtsManager

// 2. ---- ViewModel implementation ---------------------------------------------
class ElzaViewModel(
    private val stt: SpeechRecognizerManager,
    private val tts: TtsManager,
) : ViewModel() {

    // UI state exposed to ElzaScreen
    private val _uiState = MutableStateFlow(ElzaScreenState())
    val uiState: StateFlow<ElzaScreenState> = _uiState

    // 2.1 ---- STT callbacks ----------------------------------------------------
    fun startListening() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                status = "Klausos…",
                isListening = true,
                partialText = "",
                finalText = ""
            )
            stt.startListening(object : SpeechRecognizerManager.Callbacks {
                override fun onPartial(text: String) {
                    _uiState.value = _uiState.value.copy(partialText = text)
                }

                override fun onFinal(text: String) {
                    _uiState.value = _uiState.value.copy(
                        finalText = text,
                        isListening = false,
                        status = "Atpazīts."
                    )
                }

                override fun onStatus(status: String) {
                    _uiState.value = _uiState.value.copy(status = status)
                }

                override fun onError(messageLv: String) {
                    _uiState.value = _uiState.value.copy(
                        status = "Kļūda: $messageLv",
                        isListening = false
                    )
                }
            })
        }
    }

    fun stopListening() {
        stt.stopListening()
        _uiState.value = _uiState.value.copy(
            isListening = false,
            status = "Apturēts."
        )
    }

    // 2.2 ---- TTS callback -----------------------------------------------------
    fun speakTest(text: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(status = "Tiek atskaņots tests…")
            tts.speak(text) { ok ->
                _uiState.value = _uiState.value.copy(
                    status = if (ok) "Pabeigts." else "TTS kļūda."
                )
            }
        }
    }

    // 2.3 ---- Lifecycle --------------------------------------------------------
    override fun onCleared() {
        super.onCleared()
        stt.release()
        tts.release()
    }
}
