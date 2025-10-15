// ============================================================================
// File: app/src/main/java/lv/mariozo/homeogo/voice/tts/TtsEngine.kt
// Module: HomeoGO
// Purpose: Common TTS engine interface (contract for all speech engines)
// Created: 27.sep.2025 16:00 (Europe/Riga)
// ver. 1.0
// Author: Māris + ChatGPT
// ============================================================================
package lv.mariozo.homeogo.voice.tts

interface TtsEngine {
    suspend fun speak(text: String): Result<Unit>
    fun name(): String
}
