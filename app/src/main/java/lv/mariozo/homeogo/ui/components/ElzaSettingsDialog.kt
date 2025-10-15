//
// ============================================================================
// File: app/src/main/java/lv/mariozo/homeogo/ui/components/ElzaSettingsDialog.kt
// Module: HomeoGO
// Purpose: Composable dialog for adjusting Elza settings (TTS engine, speech rate, theme)
// Created: 28.sep.2025 20:00
// ver. 1.0 (initial)
// ============================================================================
// File: app/src/main/java/lv/mariozo/homeogo/ui/components/ElzaSettingsDialog.kt
// ver. 1.1 – fixed imports/types, works with Compose Material3

package lv.mariozo.homeogo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lv.mariozo.homeogo.ui.state.ElzaSettings
import lv.mariozo.homeogo.ui.state.TtsChoice
import lv.mariozo.homeogo.ui.state.ThemeChoice

@Composable
fun ElzaSettingsDialog(
    initial: ElzaSettings,
    onDismiss: () -> Unit,
    onApply: (ElzaSettings) -> Unit,
) {
    var tmp by remember { mutableStateOf(initial) }
    var ttsMenuOpen by remember { mutableStateOf(false) }
    var themeMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onApply(tmp) }) { Text("Labi") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Atcelt") } },
        title = { Text("Iestatījumi") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // — Balss dzinējs —
                Column {
                    Text("Balss dzinējs", style = MaterialTheme.typography.labelLarge)
                    Box {
                        OutlinedButton(onClick = { ttsMenuOpen = true }) {
                            Text(
                                when (tmp.ttsChoice) {
                                    TtsChoice.Elza -> "Elza (Azure)"
                                    TtsChoice.System -> "Sistēmas TTS"
                                }
                            )
                        }
                        DropdownMenu(
                            expanded = ttsMenuOpen,
                            onDismissRequest = { ttsMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Elza (Azure)") },
                                onClick = {
                                    tmp = tmp.copy(ttsChoice = TtsChoice.Elza)
                                    ttsMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sistēmas TTS") },
                                onClick = {
                                    tmp = tmp.copy(ttsChoice = TtsChoice.System)
                                    ttsMenuOpen = false
                                }
                            )
                        }
                    }
                }

                // — Runas ātrums —
                Column {
                    Text(
                        "Runas ātrums: ${"%.2f".format(tmp.speechRate)}×",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = tmp.speechRate,
                        onValueChange = { tmp = tmp.copy(speechRate = it) },
                        valueRange = 0.5f..1.5f,
                        steps = 10
                    )
                }

                // — Tēma —
                Column {
                    Text("Tēma", style = MaterialTheme.typography.labelLarge)
                    Box {
                        OutlinedButton(onClick = { themeMenuOpen = true }) {
                            Text(
                                when (tmp.themeChoice) {
                                    ThemeChoice.System -> "Sistēmas"
                                    ThemeChoice.Light -> "Gaišā"
                                    ThemeChoice.Dark -> "Tumšā"
                                }
                            )
                        }
                        DropdownMenu(
                            expanded = themeMenuOpen,
                            onDismissRequest = { themeMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sistēmas") },
                                onClick = {
                                    tmp = tmp.copy(themeChoice = ThemeChoice.System)
                                    themeMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Gaišā") },
                                onClick = {
                                    tmp = tmp.copy(themeChoice = ThemeChoice.Light)
                                    themeMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tumšā") },
                                onClick = {
                                    tmp = tmp.copy(themeChoice = ThemeChoice.Dark)
                                    themeMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}
