// File: app/src/main/java/lv/mariozo/homeogo/ui/SettingsScreen.kt
// Project: HomeoGO
// Created: 14.okt.2025 - 20:30
// ver. 1.8 (SPS-32: Add Compose Preview for SettingsScreen)
// Purpose: Settings screen UI (Compose)
// Author: Gemini Agent (Burtnieks & Elza Assistant)
// Comments:
//  - Added a @Preview Composable to enable the Design view in Android Studio.
//  - Refactored to use a stateless `SettingsScreenContent` composable.
//  - The stateful `SettingsScreen` wrapper now collects state and passes it down.
//  - The @Preview now calls the stateless content function with sample data, fixing the render error.

package lv.mariozo.homeogo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import lv.mariozo.homeogo.logic.SettingsRepository
import lv.mariozo.homeogo.ui.theme.HomeoGOTheme

// #1. ---- Stateful Wrapper Composable ---------------------------------------------
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onClose: () -> Unit,
) {
    val state by vm.ui.collectAsState()

    SettingsScreenContent(
        state = state,
        onClose = onClose,
        onSetDarkTheme = vm::setDarkTheme,
        onSetVadSensitivity = vm::setVadSensitivity,
        onSetEndpointMs = { vm.setEndpointMs(it.toInt()) },
        onSetInputGainDb = { vm.setInputGainDb(it.toInt()) },
        onSetVoice = vm::setVoice,
        onSetRate = vm::setRate,
        onSetPitch = vm::setPitch
    )
}

// #2. ---- Stateless UI Composable ------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    state: SettingsRepository.UiSnapshot,
    onClose: () -> Unit,
    onSetDarkTheme: (Boolean) -> Unit,
    onSetVadSensitivity: (Float) -> Unit,
    onSetEndpointMs: (Float) -> Unit,
    onSetInputGainDb: (Float) -> Unit,
    onSetVoice: (String) -> Unit,
    onSetRate: (Float) -> Unit,
    onSetPitch: (Float) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Iestatījumi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atpakaļ")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SectionTitle("Izskats")
            SettingRow(
                title = "Tumšais / gaišais režīms",
            ) {
                Switch(checked = state.darkTheme, onCheckedChange = onSetDarkTheme)
            }

            HorizontalDivider()

            SectionTitle("STT (runas atpazīšana)")
            SliderRow(
                title = "Jutība (VAD)",
                value = state.sttVadSensitivity,
                valueText = "%.2f".format(state.sttVadSensitivity),
                onChange = onSetVadSensitivity,
                range = 0f..1f
            )
            SliderRow(
                title = "Beigu klusums (ms)",
                value = state.sttEndpointMs.toFloat(),
                valueText = "${state.sttEndpointMs} ms",
                onChange = onSetEndpointMs,
                range = 300f..5000f
            )
            SliderRow(
                title = "Ievades pastiprinājums (dB)",
                value = state.sttInputGainDb.toFloat(),
                valueText = "${state.sttInputGainDb} dB",
                onChange = onSetInputGainDb,
                range = -20f..20f
            )

            HorizontalDivider()

            SectionTitle("TTS (Elzas balss)")
            VoicePicker(
                current = state.ttsVoiceId,
                onPick = onSetVoice,
                options = listOf(
                    "lv-LV-EveritaNeural",
                    "lv-LV-NilsNeural",
                    "en-US-JennyNeural",
                    "en-GB-RyanNeural"
                )
            )
            SliderRow(
                title = "Ātrums",
                value = state.ttsRate,
                valueText = "%.2f×".format(state.ttsRate),
                onChange = onSetRate,
                range = 0.5f..1.5f
            )
            SliderRow(
                title = "Augstums (semitoni)",
                value = state.ttsPitch,
                valueText = "%.1f".format(state.ttsPitch),
                onChange = onSetPitch,
                range = -6f..6f
            )

            /* // SPS-29: Commented out as requested.
            HorizontalDivider()

            SectionTitle("Uzvedība")
            SettingRow(
                title = "Pārtraukt ar balsi",
                subtitle = "STT klausās arī Elzas runas laikā",
            ) {
                Switch(checked = state.enableBargeIn, onCheckedChange = vm::setEnableBargeIn)
            }
            SettingRow(
                title = "Klusais režīms pēc noklusējuma",
                subtitle = "Elza atbild tikai tekstā (TTS izslēgts)",
            ) {
                Switch(checked = state.defaultMuteMode, onCheckedChange = vm::setDefaultMute)
            }
            */

            Spacer(Modifier.height(24.dp))
        }
    }
}


// #3. ---- UI Sub-components -----------------------------------------------------

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing()
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    valueText: String,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(valueText, style = MaterialTheme.typography.labelMedium)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoicePicker(current: String, onPick: (String) -> Unit, options: List<String>) {
    Column(Modifier.fillMaxWidth()) {
        Text("Balss", style = MaterialTheme.typography.titleSmall)
        options.forEach { id ->
            val selected = id == current
            FilterChip(
                selected = selected,
                onClick = { onPick(id) },
                label = { Text(id) },
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

// #4. ---- Preview ----------------------------------------------------------------

@Preview(showBackground = true, name = "SettingsScreen Preview")
@Composable
private fun PreviewSettingsScreen() {
    HomeoGOTheme {
        SettingsScreenContent(
            state = SettingsRepository.UiSnapshot(
                darkTheme = true,
                sttVadSensitivity = 0.6f,
                sttEndpointMs = 1200,
                sttInputGainDb = -2,
                ttsVoiceId = "lv-LV-EveritaNeural",
                ttsRate = 1.1f,
                ttsPitch = -1.0f
            ),
            onClose = {},
            onSetDarkTheme = {},
            onSetVadSensitivity = {},
            onSetEndpointMs = {},
            onSetInputGainDb = {},
            onSetVoice = {},
            onSetRate = {},
            onSetPitch = {}
        )
    }
}