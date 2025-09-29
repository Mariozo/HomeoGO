//
// ============================================================================
// File: app/src/main/java/lv/lv.mariozo.homeogo.ui.state/ElzaSettings.kt
// Module: HomeoGO
// Purpose: Data model for Elza UI settings (TTS choice, theme, speech rate, etc.)
// Created: 28.sep.2025 20:20
// ver. 1.0 (initial)
// ============================================================================
package lv.mariozo.homeogo.ui.state

import androidx.compose.runtime.saveable.listSaver

enum class TtsChoice { Elza, System }
enum class ThemeChoice { System, Light, Dark }

data class ElzaSettings(
    val ttsChoice: TtsChoice = TtsChoice.Elza,
    val themeChoice: ThemeChoice = ThemeChoice.System,
    val speechRate: Float = 1.0f, // 0.5x..1.5x
)

fun settingsSaver() = listSaver<ElzaSettings, Any>(
    save = { listOf(it.ttsChoice.name, it.themeChoice.name, it.speechRate) },
    restore = {
        ElzaSettings(
            ttsChoice = runCatching { TtsChoice.valueOf(it[0] as String) }.getOrDefault(TtsChoice.Elza),
            themeChoice = runCatching { ThemeChoice.valueOf(it[1] as String) }.getOrDefault(
                ThemeChoice.System
            ),
            speechRate = (it[2] as Number).toFloat()
        )
    }
)
