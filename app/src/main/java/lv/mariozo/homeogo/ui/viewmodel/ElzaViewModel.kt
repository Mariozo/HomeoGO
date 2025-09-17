
// File: java/lv/mariozo/homeogo/viewmodel/ElzaViewModel.kt
// Module: HomeoGO
// Purpose: ViewModel bridging UI and voice managers (STT/TTS).
// Created: 17.sep.2025 23:15
// ver. 1.2 (More diagnostic status updates added)

package lv.mariozo.homeogo.ui.viewmodel // Changed package to match directory structure

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import lv.mariozo.homeogo.speech.SpeechRecognizerManager // Corrected import
import lv.mariozo.homeogo.voice.TTSManager // This import is correct

/**
 * ElzaViewModel mediates between UI and the voice managers (STT/TTS).
 * Keeps UI state as StateFlows.
 */
class ElzaViewModel(app: Application) : AndroidViewModel(app) { // Changed this line

    private val stt = SpeechRecognizerManager(app)
    private val tts = TTSManager(app)

    private val _status = MutableStateFlow("Gatava klausīties")
    val status: StateFlow<String> = _status

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    var isListening: Boolean = false
        private set

    init {
        // Wire STT callbacks to state
        stt.onStatusChanged = { msg ->
            viewModelScope.launch { _status.emit(msg) }
        }
        stt.onPartial = { partial ->
            viewModelScope.launch {
                _recognizedText.emit("PARTIAL: $partial") // Diagnostic prefix for text field
                _status.emit("EVM: Partial: '$partial'")  // Diagnostic status update
            }
        }
        stt.onFinal = { text ->
            isListening = false
            viewModelScope.launch {
                _recognizedText.emit("FINAL: $text") // Diagnostic prefix for text field
                // Diagnostic status update (will temporarily override the original status update here)
                _status.emit("EVM: Final: '$text'") 
                // Original status update (commented out for diagnostics):
                // _status.emit(if (text.isNotBlank()) "Gatava klausīties" else "Nekas netika atpazīts")
            }
        }
        stt.onError = { code ->
            isListening = false
            viewModelScope.launch { _status.emit("Kļūda ($code)") }
        }
    }

    fun setStatus(s: String) {
        viewModelScope.launch { _status.emit(s) }
    }

    fun startListening() {
        isListening = true
        viewModelScope.launch {
            _recognizedText.emit("") // Clear previous text
            stt.start()
        }
    }

    fun stopListening() {
        isListening = false
        stt.stop()
        viewModelScope.launch { _status.emit("Gatava klausīties") }
    }

    fun speakReply() {
        val txt = recognizedText.value
        val reply = if (txt.isNotBlank()) "Tu teici: $txt" else "Nav ko nolasīt."
        tts.speak(reply)
    }

    override fun onCleared() {
        super.onCleared()
        stt.release()
        tts.release()
    }
}
