// File: app/src/main/java/lv/mariozo/homeogo/logic/SettingsRepository.kt
// Project: HomeoGO
// Created: 17.okt.2025 - 11:25 (Europe/Riga)
// ver. 1.2 (SPS-15: Fix companion object visibility)
// Purpose: Repository for managing app settings using DataStore.
// Author: Gemini Agent (Burtnieks & Elza Assistant)
// Comments:
//  - Removed the `private` modifier from the companion object to allow access from the ViewModel.

package lv.mariozo.homeogo.logic

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository private constructor(context: Context) {

    private val dataStore = context.dataStore

    val settingsFlow: Flow<UiSnapshot> = dataStore.data
        .map { prefs ->
            UiSnapshot(
                // Behavior
                enableBargeIn = prefs[ENABLE_BARGE_IN] ?: false,
                defaultMuteMode = prefs[DEFAULT_MUTE_MODE] ?: false,
                // Appearance
                darkTheme = prefs[DARK_THEME] ?: false,
                // STT
                sttVadSensitivity = prefs[STT_VAD_SENSITIVITY] ?: 0.5f,
                sttEndpointMs = prefs[STT_ENDPOINT_MS] ?: 1500,
                sttInputGainDb = prefs[STT_INPUT_GAIN_DB] ?: 0,
                // TTS
                ttsVoiceId = prefs[TTS_VOICE_ID] ?: "lv-LV-EveritaNeural",
                ttsRate = prefs[TTS_RATE] ?: 1.0f,
                ttsPitch = prefs[TTS_PITCH] ?: 0.0f
            )
        }

    // --- Behavior ---
    suspend fun setEnableBargeIn(enabled: Boolean) {
        dataStore.edit { it[ENABLE_BARGE_IN] = enabled }
    }

    suspend fun setDefaultMute(enabled: Boolean) {
        dataStore.edit { it[DEFAULT_MUTE_MODE] = enabled }
    }

    // --- Appearance ---
    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { it[DARK_THEME] = enabled }
    }

    // --- STT ---
    suspend fun setVadSensitivity(value: Float) {
        dataStore.edit { it[STT_VAD_SENSITIVITY] = value }
    }

    suspend fun setEndpointMs(value: Int) {
        dataStore.edit { it[STT_ENDPOINT_MS] = value }
    }

    suspend fun setInputGainDb(value: Int) {
        dataStore.edit { it[STT_INPUT_GAIN_DB] = value }
    }

    // --- TTS ---
    suspend fun setVoice(id: String) {
        dataStore.edit { it[TTS_VOICE_ID] = id }
    }

    suspend fun setRate(value: Float) {
        dataStore.edit { it[TTS_RATE] = value }
    }

    suspend fun setPitch(value: Float) {
        dataStore.edit { it[TTS_PITCH] = value }
    }


    data class UiSnapshot(
        // Behavior
        val enableBargeIn: Boolean = false,
        val defaultMuteMode: Boolean = false,
        // Appearance
        val darkTheme: Boolean = false,
        // STT
        val sttVadSensitivity: Float = 0.5f,
        val sttEndpointMs: Int = 1500,
        val sttInputGainDb: Int = 0,
        // TTS
        val ttsVoiceId: String = "lv-LV-EveritaNeural",
        val ttsRate: Float = 1.0f,
        val ttsPitch: Float = 0.0f,
    )

    companion object {
        // Behavior
        private val ENABLE_BARGE_IN = booleanPreferencesKey("enable_barge_in")
        private val DEFAULT_MUTE_MODE = booleanPreferencesKey("default_mute_mode")

        // Appearance
        private val DARK_THEME = booleanPreferencesKey("dark_theme")

        // STT
        private val STT_VAD_SENSITIVITY = floatPreferencesKey("stt_vad_sensitivity")
        private val STT_ENDPOINT_MS = intPreferencesKey("stt_endpoint_ms")
        private val STT_INPUT_GAIN_DB = intPreferencesKey("stt_input_gain_db")

        // TTS
        private val TTS_VOICE_ID = stringPreferencesKey("tts_voice_id")
        private val TTS_RATE = floatPreferencesKey("tts_rate")
        private val TTS_PITCH = floatPreferencesKey("tts_pitch")


        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context).also { INSTANCE = it }
            }
        }
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
