// File: app/src/main/java/lv/mariozo/homeogo/ui/SettingsViewModel.kt
// Project: HomeoGO
// Created: 17.okt.2025 - 10:45 (Europe/Riga)
// ver. 2.4 (SPS-10: Force IDE re-index)
// Purpose: ViewModel for the Settings screen.
// Author: Gemini Agent (Burtnieks & Elza Assistant)
// Comments:
//  - This is a minor, non-functional change to force the IDE to re-index the file.

package lv.mariozo.homeogo.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lv.mariozo.homeogo.logic.SettingsRepository

class SettingsViewModel(private val app: Application) : ViewModel() {

    private val repo = SettingsRepository.getInstance(app)

    val ui: StateFlow<SettingsRepository.UiSnapshot> = repo.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsRepository.UiSnapshot()
        )

    // --- Behavior ---
    fun setEnableBargeIn(enabled: Boolean) {
        viewModelScope.launch { repo.setEnableBargeIn(enabled) }
    }
    fun setDefaultMute(enabled: Boolean) {
        viewModelScope.launch { repo.setDefaultMute(enabled) }
    }

    // --- Appearance ---
    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { repo.setDarkTheme(enabled) }
    }

    // --- STT ---
    fun setVadSensitivity(value: Float) {
        viewModelScope.launch { repo.setVadSensitivity(value) }
    }
    fun setEndpointMs(value: Int) {
        viewModelScope.launch { repo.setEndpointMs(value) }
    }
    fun setInputGainDb(value: Int) {
        viewModelScope.launch { repo.setInputGainDb(value) }
    }

    // --- TTS ---
    fun setVoice(id: String) {
        viewModelScope.launch { repo.setVoice(id) }
    }
    fun setRate(value: Float) {
        viewModelScope.launch { repo.setRate(value) }
    }
    fun setPitch(value: Float) {
        viewModelScope.launch { repo.setPitch(value) }
    }

    fun snapshot(): SettingsRepository.UiSnapshot = ui.value

    // --- Factory for creating ViewModel with Application context ---
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return SettingsViewModel(application) as T
            }
        }
    }
}
