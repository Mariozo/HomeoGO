// File: app/src/main/java/lv/mariozo/homeogo/voice/TTSManager.kt
// Module: HomeoGO
// Purpose: Android TextToSpeech wrapper for Elza (Latvian voice preferred)
// Created: 17.sep.2025   14:33
// ver. 1.5 (Removed unresolved KEY_FEATURE_VOICE_GENDER_FEMALE, relying on string "female")

// File: app/src/main/java/lv/mariozo/homeogo/voice/TTSManager.kt
// Module: HomeoGO
// Purpose: Android TextToSpeech wrapper for Elza (Latvian voice preferred)
// Created: 17.sep.2025   14:33
// ver. 1.5 (Removed unresolved KEY_FEATURE_VOICE_GENDER_FEMALE, relying on string "female")

package lv.mariozo.homeogo.voice

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

class TTSManager(
    context: Context,
    preferredEnginePackage: String? = "com.google.android.tts",
) {

    private var tts: TextToSpeech? = null
    private var isReady: Boolean = false

    init {
        val listener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureLatvianVoice()
                isReady = true
            } else {
                Log.e("TTSManager", "TTS init failed: status=$status")
            }
        }

        tts = try {
            if (preferredEnginePackage != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                TextToSpeech(context, listener, preferredEnginePackage)
            } else {
                TextToSpeech(context, listener)
            }
        } catch (e: Throwable) {
            Log.w(
                "TTSManager",
                "Preferred engine not available, falling back to default: ${e.message}"
            )
            TextToSpeech(context, listener)
        }
    }

    private fun configureLatvianVoice() {
        val engine = tts ?: return
        val lv = Locale("lv", "LV")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val voices = engine.voices
            if (voices != null) {
                val lvVoices = voices
                    .filter { it.locale.language.equals("lv", ignoreCase = true) }
                    .sortedWith(
                        compareByDescending<Voice> { it.features.contains("female") }
                            .thenByDescending { it.name.contains("google", ignoreCase = true) }
                            .thenByDescending { if (Build.VERSION.SDK_INT >= 22) it.quality else 0 }
                            .thenBy { it.name }
                    )
                for (v in lvVoices) {
                    val res = engine.setVoice(v as? Voice ?: continue)
                    if (res == TextToSpeech.SUCCESS) {
                        Log.i(
                            "TTSManager",
                            "Using LV voice: ${v.name} (${v.locale}, features: ${v.features})"
                        )
                        engine.setSpeechRate(1.0f)
                        engine.setPitch(1.0f)
                        return
                    }
                }
            }
        }

        val langRes = engine.setLanguage(lv)
        if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("TTSManager", "Latvian not supported; falling back to device default")
            engine.language = Locale.getDefault()
        } else {
            engine.setSpeechRate(1.0f)
            engine.setPitch(1.0f)
        }
    }

    fun speak(text: String) {
        if (!isReady || text.isBlank()) return
        val engine = tts ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "elza-utterance-id")
        } else {
            @Suppress("DEPRECATION")
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    fun isLatvianAvailable(): Boolean {
        val engine = tts ?: return false
        val lv = Locale("lv", "LV")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            engine.voices?.let { vs ->
                if (vs.any { it.locale.language.equals("lv", true) }) return true
            }
        }
        val r = engine.isLanguageAvailable(lv)
        return (r == TextToSpeech.LANG_AVAILABLE
                || r == TextToSpeech.LANG_COUNTRY_AVAILABLE
                || r == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE)
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
