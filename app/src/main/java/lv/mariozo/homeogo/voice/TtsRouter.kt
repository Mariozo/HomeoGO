// ============================================================================
// File: app/src/main/java/lv/mariozo/homeogo/voice/TtsRouter.kt
// Module: HomeoGO
// Created:  27.sep.2025 16:00
// ver. 2.1  27.okt.2025 09:30
// Purpose: TTS router to pick active engine (System/Azure/etc.)
// Notes   • Added a secure router between System TTS and Azure TTS,
//         • Azure keys/regions are taken from BuildConfig (generated from local.properties),
//         • If keys are not set - Azure TTS is disabled with an explicit error,
//         • Small @Composable wrapper (rememberTtsRouter) for easy injection into the UI layer.
// ============================================================================

package lv.mariozo.homeogo.voice

// #1. ---- Stateful Wrapper Composable ---------------------------------------------
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import lv.mariozo.homeogo.BuildConfig
import lv.mariozo.homeogo.voice.tts.TtsEngine
import lv.mariozo.homeogo.voice.tts.azure.AzureTtsEngine
import lv.mariozo.homeogo.voice.tts.system.SystemTtsEngine

/**
 * Ērts @Composable ietinamais, lai UI slānī iegūtu vienu TtsRouter eksemplāru.
 * Ja negribi Compose atkarību šajā failā, šo funkciju vari pārvietot uz UI pakotni.
 */
@Composable
fun rememberTtsRouter(): TtsRouter {
    val ctx = LocalContext.current
    return remember(ctx) { TtsRouter(ctx) }
}

// #2. ---- Router core (engines, selection, API) ------------------------------------
class TtsRouter(private val context: Context) {

    enum class Engine { System, Azure }

    private val logTag = "TtsRouter"

    // System TTS vienmēr pieejams
    private val systemEngine: TtsEngine by lazy { SystemTtsEngine(context) }

    // Azure TTS — tikai ja atslēgas/regions ir iestatīti
    private val azureEngine: TtsEngine by lazy {
        val key = BuildConfig.AZURE_SPEECH_KEY
        val region = BuildConfig.AZURE_SPEECH_REGION

        if (key.isNullOrBlank() || region.isNullOrBlank()) {
            Log.w(logTag, "Azure TTS is DISABLED (missing key/region in BuildConfig)")
            object : TtsEngine {
                override fun name() = "AzureTTS(disabled)"
                override suspend fun speak(text: String): Result<Unit> =
                    Result.failure(IllegalStateException("Azure TTS atslēga/regions nav iestatīti"))
            }
        } else {
            AzureTtsEngine(
                context = context,
                key = key,
                region = region,
                voice = "lv-LV-EveritaNeural" // you can change, e.g. "en-LV-NilsNeural"
            )
        }
    }

    /** Active engine (System by default, as it is always available) */
    var current: Engine = Engine.System

    /** Current TTS engine. */
    fun engine(): TtsEngine = when (current) {
        Engine.System -> systemEngine
        Engine.Azure -> azureEngine
    }

    /** Convenient delegator for talking through the selected engine. */
    suspend fun speak(text: String): Result<Unit> = engine().speak(text)

}
