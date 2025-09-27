// File: app/src/main/java/lv/mariozo/homeogo/voice/TtsRouter.kt
// Module: HomeoGO
// Purpose: Single entry point to route TTS calls (Azure vs System)
// Created: 24.sep.2025
// ver. 1.9.1

package lv.mariozo.homeogo.voice

import android.content.Context

// AzureTts is in the same package (lv.mariozo.homeogo.voice)

interface TtsEngine {
    fun speak(text: String)
    fun close() {}
}

class SystemTtsEngine(private val impl: TTSManager) : TtsEngine {
    override fun speak(text: String) =
        impl.speak(text) // Assuming TTSManager has a speak(String) method

    override fun close() {
        // If TTSManager has a shutdown or close method, it should be called here.
        // For example: if (impl is AutoCloseable) impl.close()
        // Or if it has a specific shutdown method: impl.shutdown()
    }
}

class AzureTtsEngine(
    private val azureTtsObject: AzureTts, // Renamed for clarity, this is the AzureTts singleton
    private val context: Context,
    private val key: String,
    private val region: String,
) : TtsEngine {
    override fun speak(text: String) {
        azureTtsObject.speak(context, text, key, region) { result ->
            // The TtsEngine.speak interface returns Unit.
            // You can log the result or handle errors/status here.
            println("AzureTTS synthesis result: $result")
        }
    }
    // No explicit close() override needed as AzureTts object doesn't have one;
    // TtsEngine interface provides a default empty implementation.
}

enum class TtsChoice { Elza, System }

class TtsRouter(
    private val system: SystemTtsEngine,
    private val elza: AzureTtsEngine,
) {
    fun speak(choice: TtsChoice, text: String) = when (choice) {
        TtsChoice.System -> system.speak(text)
        TtsChoice.Elza -> elza.speak(text)
    }

    fun close() {
        system.close()
        elza.close() // This will call the (default or overridden) close for AzureTtsEngine
    }
}
