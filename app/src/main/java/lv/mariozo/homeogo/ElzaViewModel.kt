// File: app/src/main/java/lv/mariozo/homeogo/ui/ElzaViewModel.kt
// Project: HomeoGO
// Created: 03.okt.2025 12:00 (Rīga)
// ver. 1.2
// Purpose: ViewModel for ElzaScreen. Connects UI state to SpeechRecognizerManager (STT)
//          and TtsManager (TTS). Exposes start/stop/speakTest methods.
// Comments:
//  - Injects Azure credentials via BuildConfig into managers.
//  - Holds UI state as StateFlow for Compose UI.
//  - Lifecycle-aware cleanup via onCleared().

package lv.mariozo.homeogo.ui

// 1. ---- Imports ---------------------------------------------------------------
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import lv.mariozo.homeogo.BuildConfig
import lv.mariozo.homeogo.voice.SpeechRecognizerManager
import lv.mariozo.homeogo.voice.TtsManager

// 2. ---- UI State --------------------------------------------------------------
data class ElzaScreenState(
    val status: String = "Idle",
    val recognizedText: String = "",
    val isListening: Boolean = false,
)

// 3. ---- ViewModel -------------------------------------------------------------
class ElzaViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ElzaScreenState())
    val uiState: StateFlow<ElzaScreenState> = _uiState

    private val sttManager: SpeechRecognizerManager = SpeechRecognizerManager(
        context = application.applicationContext,
        speechKey = BuildConfig.AZURE_SPEECH_KEY,
        speechRegion = BuildConfig.AZURE_SPEECH_REGION
    )

    private val ttsManager: TtsManager = TtsManager(
        speechKey = BuildConfig.AZURE_SPEECH_KEY,
        speechRegion = BuildConfig.AZURE_SPEECH_REGION
    )

    // 3.1 ---- STT control ------------------------------------------------------
    fun startListening() {
        _uiState.value = _uiState.value.copy(status = "Klausos...", isListening = true)
        sttManager.startListening(object : SpeechRecognizerManager.Callbacks {
            override fun onPartial(text: String) {
                _uiState.value =
                    _uiState.value.copy(status = "Dzird daļu...", recognizedText = text)
            }

            override fun onFinal(text: String) {
                _uiState.value = _uiState.value.copy(
                    status = "Atpazīts!",
                    recognizedText = text,
                    isListening = false
                )
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

    // 3.2 ---- TTS control ------------------------------------------------------
    fun speakTest(text: String) {
        _uiState.value = _uiState.value.copy(status = "Runā: $text")
        ttsManager.speak(text) { ok ->
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    status = if (ok) "Atskaņošana pabeigta" else "TTS kļūda"
                )
            }
        }
    }

    // 3.3 ---- Cleanup -----------------------------------------------------------
    override fun onCleared() {
        super.onCleared()
        sttManager.release()
        ttsManager.release()
    }
}
