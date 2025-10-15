// ============================================================================
// File: app/src/main/java/lv/mariozo/homeogo/voice/TtsRouter.kt
// Module: HomeoGO
// Purpose: TTS router to pick active engine (System/Azure/etc.)
// Created: 27.sep.2025 16:00 (Europe/Riga)
// ver. 2.0 (clean, no embedded engines)
// ============================================================================
package lv.mariozo.homeogo.voice

import lv.mariozo.homeogo.voice.tts.TtsEngine

class TtsRouter(
    private val engines: List<TtsEngine>,
    private val preferred: String? = null,
) {
    fun current(): TtsEngine =
        engines.firstOrNull { it.name() == preferred } ?: engines.first()

    suspend fun speak(text: String): Result<Unit> = current().speak(text)
}
