// File: app/src/main/java/lv/mariozo/homeogo/voice/TtsRouter.kt
// Module: HomeoGO
// Purpose: Single entry point to route TTS calls (Azure vs System)
// Created: 24.sep.2025
// ver. 1.9.1

package lv.mariozo.homeogo.voice

interface TtsEngine {
    fun speak(text: String)
    fun close() {}
}

class SystemTtsEngine(private val impl: TTSManager) : TtsEngine {
    override fun speak(text: String) = impl.speak(text)
    override fun close() {
        // no-op for now; add impl.shutdown() here if your TTSManager exposes it
    }
}

class AzureTtsEngine(private val impl: AzureTts) : TtsEngine {
    override fun speak(text: String) = impl.speakBlocking(text)
    override fun close() = impl.close()
}

enum class TtsChoice { Elza, System }

class TtsRouter(
    private val system: SystemTtsEngine,
    private val elza: AzureTtsEngine
) {
    fun speak(choice: TtsChoice, text: String) = when (choice) {
        TtsChoice.System -> system.speak(text)
        TtsChoice.Elza   -> elza.speak(text)
    }
    fun close() {
        system.close()
        elza.close()
    }
}
