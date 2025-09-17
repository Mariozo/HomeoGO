
package lv.mariozo.homeogo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
// Removed incorrect: import kotlinx.coroutines.flow.launch
import kotlinx.coroutines.launch // Added correct import for launch, though viewModelScope.launch often suffices
import lv.mariozo.homeogo.speech.SpeechRecognizerManager
import lv.mariozo.homeogo.voice.TTSManager

data class ElzaUiState(
    val status: String = "Idle",
    val recognizedText: String = "",
    val isListening: Boolean = false
)

class ElzaViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(ElzaUiState())
    val uiState = _uiState.asStateFlow()

    private val tts = TTSManager(app)
    private val srm = SpeechRecognizerManager(context = app, scope = viewModelScope)

    init {
        viewModelScope.launch {
            srm.status.collect { srmStatus ->
                _uiState.value = _uiState.value.copy(status = mapSrmStatusToUiStatus(srmStatus))
            }
        }

        viewModelScope.launch {
            srm.isListening.collect { listening ->
                val currentStatus = _uiState.value.status
                _uiState.value = _uiState.value.copy(
                    isListening = listening,
                    status = if (listening) "Listening..." else if (currentStatus == "Listening...") "Idle" else currentStatus
                )
            }
        }

        srm.onPartial = { partialText ->
            _uiState.value = _uiState.value.copy(
                recognizedText = partialText,
                status = if (_uiState.value.isListening) "Partial..." else _uiState.value.status
            )
        }

        srm.onFinal = { finalText ->
            _uiState.value = _uiState.value.copy(
                recognizedText = finalText,
                status = if (finalText.isNotEmpty()) "Final ✓" else "Nothing recognized",
                isListening = false
            )
        }

        srm.onError = { errorCode ->
            _uiState.value = _uiState.value.copy(
                status = "Error (code: $errorCode)",
                isListening = false
            )
        }
    }

    private fun mapSrmStatusToUiStatus(srmStatus: String): String {
        return when {
            srmStatus.startsWith("SRM_ON_ERROR_CODE:") -> "Error processing speech"
            srmStatus.startsWith("SRM_FINAL_RAW:") -> "Processing result..."
            srmStatus.startsWith("SRM_PARTIAL_RAW:") && _uiState.value.isListening -> "Listening..."
            srmStatus == "Idle" && !_uiState.value.isListening -> "Idle"
            srmStatus.contains("Listening…", ignoreCase = true) && _uiState.value.isListening -> "Listening..."
            srmStatus.contains("Speak!", ignoreCase = true) && _uiState.value.isListening -> "Speak now"
            srmStatus.contains("Processing…", ignoreCase = true) -> "Processing..."
            srmStatus == "Idle" && (_uiState.value.status.contains("Error") || _uiState.value.status.contains("Final")) -> _uiState.value.status
            else -> if (_uiState.value.isListening) "Listening..." else "Idle"
        }
    }

    fun startListening() {
        srm.start()
        _uiState.value = _uiState.value.copy(recognizedText = "")
    }

    fun stopListening() {
        srm.stop()
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
        _uiState.value = _uiState.value.copy(status = "Microphone permission denied", isListening = false)
    }

    override fun onCleared() {
        super.onCleared()
        tts.release()
        srm.release()
    }
}
