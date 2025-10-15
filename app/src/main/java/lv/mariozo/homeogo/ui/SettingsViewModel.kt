// File: app/src/main/java/lv/mariozo/homeogo/ui/SettingsViewModel.kt
// Project: HomeoGO
// Created: 17.okt.2025 - 23:05 (Europe/Riga)
// ver. 3.2 — Fix: use repo.ui instead of repo.flow; no-op setInteractionMode to compile

// File: app/src/main/java/lv/mariozo/homeogo/ui/SettingsViewModel.kt
// Project: HomeoGO
// Updated: 17.okt.2025 (Europe/Riga)
// ver. 3.3 — Wire to SettingsRepository.settingsFlow; pass-through setters

package lv.mariozo.homeogo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lv.mariozo.homeogo.logic.SettingsRepository

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: SettingsRepository =
        SettingsRepository.getInstance(app.applicationContext)

    /** Publiskā straume iestatījumiem – ņemam no repo.settingsFlow */
    val ui: StateFlow<SettingsRepository.UiSnapshot> =
        repo.settingsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsRepository.UiSnapshot()
        )

    /** Ātra piekļuve pašreizējam stāvoklim (bez kolekta UI pusē). */
    fun snapshot(): SettingsRepository.UiSnapshot = ui.value

    // ───────────────────── Mutācijas (passthrough uz repo) ─────────────────────

    // Izskats
    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch {
        repo.setDarkTheme(enabled)
    }

    // Uzvedība (ja UI vēl izmanto šos slēdžus)
    fun setEnableBargeIn(enabled: Boolean) = viewModelScope.launch {
        repo.setEnableBargeIn(enabled)
    }

    fun setDefaultMute(enabled: Boolean) = viewModelScope.launch {
        repo.setDefaultMute(enabled)
    }

    // STT
    fun setVadSensitivity(value: Float) = viewModelScope.launch {
        repo.setVadSensitivity(value)
    }

    fun setEndpointMs(value: Int) = viewModelScope.launch {
        repo.setEndpointMs(value)
    }

    fun setInputGainDb(value: Int) = viewModelScope.launch {
        repo.setInputGainDb(value)
    }

    // TTS
    fun setVoice(voiceId: String) = viewModelScope.launch {
        repo.setVoice(voiceId)
    }

    fun setRate(rate: Float) = viewModelScope.launch {
        repo.setRate(rate)
    }

    fun setPitch(pitch: Float) = viewModelScope.launch {
        repo.setPitch(pitch)
    }

    /**
     * (Pagaidu) Režīma iestatītājs – ja MainActivity vēl atsaucas, lai nav “unresolved”.
     * Ja režīmu neglabā DataStore, šeit var palikt no-op.
     */
    fun setInteractionMode(@Suppress("UNUSED_PARAMETER") mode: InteractionMode) {
        // no-op; ja vēlāk glabāsi DataStore, pievienosim repo.setInteractionMode(mode)
    }

    // ───────────────────────── Factory (AndroidViewModel) ──────────────────────
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras,
            ): T {
                val app = checkNotNull(
                    extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                ) as Application
                return SettingsViewModel(app) as T
            }
        }
    }
}
