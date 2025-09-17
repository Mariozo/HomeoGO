// File: app/src/main/java/lv/mariozo/homeogo/voice/TTSManager.kt
// Module: HomeoGO
// Purpose: Android TextToSpeech wrapper for Elza (Latvian voice preferred)
// Created: 17.sep.2025   14:33
// ver. 1.5 (Removed unresolved KEY_FEATURE_VOICE_GENDER_FEMALE, relying on string "female")

// # 1.  ------ Package & Imports ---------------------------------------------
package lv.mariozo.homeogo.voice

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice // RESTORED this explicit import
import android.util.Log
import java.util.Locale

// # 2.  ------ Public API & State --------------------------------------------
/**
 * TTSManager
 * - Wraps Android TextToSpeech with preference for Latvian ("lv-LV") voice.
 * - Tries to init Google TTS engine (com.google.android.tts); falls back to default if not available.
 * - Picks a specific Latvian Voice if present; otherwise sets language to lv-LV; otherwise falls back to device default.
 */
class TTSManager(
    context: Context,
    preferredEnginePackage: String? = "com.google.android.tts" // try Google TTS first
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

        // Prefer an explicit engine if provided; otherwise default engine
        tts = try {
            if (preferredEnginePackage != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                TextToSpeech(context, listener, preferredEnginePackage)
            } else {
                TextToSpeech(context, listener)
            }
        } catch (e: Throwable) {
            Log.w("TTSManager", "Preferred engine not available, falling back to default: ${e.message}")
            TextToSpeech(context, listener)
        }
    }

    // # 3.  ------ Voice/Language selection -----------------------------------
    private fun configureLatvianVoice() {
        val engine = tts ?: return
        val lv = Locale("lv", "LV") // For older Android, Locale.forLanguageTag("lv-LV") is better if API >= 21

        // Try to pick a concrete Latvian Voice first (more reliable than setLanguage on some engines)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val voices = engine.voices
            if (voices != null) {
                // Prefer female, then Google, then by quality, then by name
                val lvVoices = voices
                    .filter { it.locale.language.equals("lv", ignoreCase = true) }
                    .sortedWith(
                        compareByDescending<Voice> {
                            // Prioritize female voices by checking for the "female" feature string directly
                            it.features.contains("female") 
                        }.thenByDescending {
                            // Then prioritize Google voices
                            it.name.contains("google", ignoreCase = true)
                        }.thenByDescending {
                            // Then by quality
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) it.quality else 0
                        }.thenBy {
                            // Finally, by name
                            it.name
                        }
                    )

                for (v in lvVoices) {
                    // Explicitly type v if inference fails, though it should be Voice
                    val res = engine.setVoice(v as? Voice ?: continue) 
                    if (res == TextToSpeech.SUCCESS) {
                        Log.i("TTSManager", "Using LV voice: ${v.name} (${v.locale}, features: ${v.features})")
                        // Optional: normalize rate/pitch if needed
                        engine.setSpeechRate(1.0f)
                        engine.setPitch(1.0f)
                        return
                    }
                }
            }
        }

        // If no specific LV voice chosen, fallback to language setting
        val langRes = engine.setLanguage(lv)
        if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("TTSManager", "Latvian language missing/not supported; falling back to device default")
            engine.language = Locale.getDefault()
        } else {
            engine.setSpeechRate(1.0f)
            engine.setPitch(1.0f)
        }
    }

    // # 4.  ------ Speak API ---------------------------------------------------
    /** Speaks given text aloud. If not initialized or text blank, call is ignored. */
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

    // # 5.  ------ Diagnostics (optional) -------------------------------------
    /** Returns true if any Latvian voice or language is available. */
    fun isLatvianAvailable(): Boolean {
        val engine = tts ?: return false
        val lv = Locale("lv", "LV")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            engine.voices?.let { vs ->
                if (vs.any { it.locale.language.equals("lv", true) }) return true
            }
        }
        val r = engine.isLanguageAvailable(lv)
        return (r == TextToSpeech.LANG_AVAILABLE || r == TextToSpeech.LANG_COUNTRY_AVAILABLE || r == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE)
    }

    // # 6.  ------ Release -----------------------------------------------------
    /** Releases TTS resources. Call from ViewModel.onCleared() or Activity.onDestroy(). */
    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
