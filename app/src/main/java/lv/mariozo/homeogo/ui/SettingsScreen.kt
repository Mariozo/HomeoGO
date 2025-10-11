// File: app/src/main/java/lv/mariozo/homeogo/ui/SettingsScreen.kt
// Project: HomeoGO
// Created: 17.okt.2025 - 10:55 (Europe/Riga)
// ver. 1.6 (SPS-11: Fix final unresolved reference)
// Purpose: Settings screen UI (Compose)
// Author: Gemini Agent (Burtnieks & Elza Assistant)
// Comments:
//  - Renamed `setBargeIn` to `setEnableBargeIn` to match the ViewModel method.

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lv.mariozo.homeogo.ui.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class) // Suppress warnings for Scaffold, TopAppBar, Slider etc.
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onClose: () -> Unit,
) {
    val s = vm.ui.collectAsState().value

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
                Switch(checked = s.darkTheme, onCheckedChange = vm::setDarkTheme)
            }

            HorizontalDivider()

            SectionTitle("STT (runas atpazīšana)")
            SliderRow(
                title = "Jutība (VAD)",
                value = s.sttVadSensitivity,
                valueText = "%.2f".format(s.sttVadSensitivity),
                onChange = vm::setVadSensitivity,
                range = 0f..1f
            )
            SliderRow(
                title = "Beigu klusums (ms)",
                value = s.sttEndpointMs.toFloat(),
                valueText = "${s.sttEndpointMs} ms",
                onChange = { vm.setEndpointMs(it.toInt()) },
                range = 300f..5000f
            )
            SliderRow(
                title = "Ievades pastiprinājums (dB)",
                value = s.sttInputGainDb.toFloat(),
                valueText = "${s.sttInputGainDb} dB",
                onChange = { vm.setInputGainDb(it.toInt()) },
                range = -20f..20f
            )

            HorizontalDivider()

            SectionTitle("TTS (Elzas balss)")
            VoicePicker(
                current = s.ttsVoiceId,
                onPick = vm::setVoice,
                options = listOf(
                    "lv-LV-EveritaNeural",
                    "lv-LV-NilsNeural",
                    "en-US-JennyNeural",
                    "en-GB-RyanNeural"
                )
            )
            SliderRow(
                title = "Ātrums",
                value = s.ttsRate,
                valueText = "%.2f×".format(s.ttsRate),
                onChange = vm::setRate,
                range = 0.5f..1.5f
            )
            SliderRow(
                title = "Augstums (semitoni)",
                value = s.ttsPitch,
                valueText = "%.1f".format(s.ttsPitch),
                onChange = vm::setPitch,
                range = -6f..6f
            )

            HorizontalDivider()

            SectionTitle("Uzvedība")
            SettingRow(
                title = "Pārtraukt ar balsi",
                subtitle = "STT klausās arī Elzas runas laikā",
            ) {
                Switch(checked = s.enableBargeIn, onCheckedChange = vm::setEnableBargeIn)
            }
            SettingRow(
                title = "Klusais režīms pēc noklusējuma",
                subtitle = "Elza atbild tikai tekstā (TTS izslēgts)",
            ) {
                Switch(checked = s.defaultMuteMode, onCheckedChange = vm::setDefaultMute)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

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
