// File: app/src/main/java/lv/mariozo/homeogo/ui/ElzaScreen.kt
// Project: HomeoGO (Android, Jetpack Compose + Material3)
// Module: app
// Purpose: Visual Elza screen (STT + TTS controls) with status, partial/final text,
//          and a small settings dialog; designed to compile cleanly and render in
//          Android Studio Preview without project-specific dependencies.
// Created: 01.okt.2025 14:18
// ver. 1.8
// Notes:
//  - Pure UI: no direct imports of Azure/TTS/STT managers; integration is via callbacks.
//  - Uses only stable Material3 APIs; HorizontalDivider (no deprecated Divider).
//  - Two previews (light/dark) use MaterialTheme to avoid project theme dependency.
//  - UI strings LV; code & comments EN. Blocks numbered per MK!.

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package lv.mariozo.homeogo.ui


// 1. ---- Imports ---------------------------------------------------------------
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// 2. ---- Public API ------------------------------------------------------------
// External callbacks allow ViewModel or managers (STT/TTS) to be plugged in.
// In Preview (no VM attached), a local demo state path is used.
@Composable
fun ElzaScreen(
    modifier: Modifier = Modifier,
    // STT controls:
    onStartListening: (() -> Unit)? = null,
    onStopListening: (() -> Unit)? = null,
    // TTS control:
    onSpeakTest: ((String) -> Unit)? = null,
    // Optional state inputs from VM. If null, local demo state is used.
    isListeningExternal: Boolean? = null,
    statusTextExternal: String? = null,
    partialTextExternal: String? = null,
    finalTextExternal: String? = null,
) {
    // 2.1 ---- Local demo state (used only if VM state not provided) ------------
    var isListening by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Gatavs.") }
    var partial by remember { mutableStateOf("") }
    var final by remember { mutableStateOf("") }
    var testPhrase by remember { mutableStateOf("Labdien! Kā Tev klājas šodien?") }

    val isListeningState = isListeningExternal ?: isListening
    val statusState = statusTextExternal ?: status
    val partialState = partialTextExternal ?: partial
    val finalState = finalTextExternal ?: final

    Surface(modifier = modifier.fillMaxSize()) {
        Column {
            // 3. ---- Top bar ----------------------------------------------------
            TopAppBar(
                title = { Text("Elza – runa un balss") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            // 4. ---- Body -------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                // 4.1 ---- Status card -------------------------------------------
                StatusCard(
                    title = "Statuss",
                    text = statusState
                )

                Spacer(Modifier.height(16.dp))

                // 4.2 ---- Controls row -----------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            // Prefer external VM callback if provided
                            if (onStartListening != null) {
                                onStartListening()
                            } else {
                                // Demo behavior (local state)
                                status = "Klausos…"
                                isListening = true
                                partial = ""
                                final = ""
                            }
                        },
                        enabled = !isListeningState
                    ) {
                        Text("Klausos")
                    }

                    OutlinedButton(
                        onClick = {
                            if (onStopListening != null) {
                                onStopListening()
                            } else {
                                status = "Apturēts."
                                isListening = false
                            }
                        },
                        enabled = isListeningState
                    ) {
                        Text("Stop")
                    }

                    Button(
                        onClick = {
                            val phrase = finalState.ifBlank { testPhrase }
                            if (onSpeakTest != null) {
                                onSpeakTest(phrase)
                            } else {
                                status = "Tiek atskaņots tests…"
                            }
                        }
                    ) {
                        Text("Pārbaudīt balsi")
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // 4.3 ---- Recognized text panels --------------------------------
                RecognizedCard(
                    header = "Daļējais (partial)",
                    text = partialState.ifBlank { "—" }
                )
                Spacer(Modifier.height(12.dp))
                RecognizedCard(
                    header = "Gala (final)",
                    text = finalState.ifBlank { "—" }
                )

                Spacer(Modifier.height(24.dp))

                // 4.4 ---- Test phrase input (for TTS) ---------------------------
                Text(
                    "Tests frāzei (TTS):",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = testPhrase,
                    onValueChange = { testPhrase = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ieraksti ko Elza nolasīs…") },
                    singleLine = true
                )

                Spacer(Modifier.height(32.dp))

                // 4.5 ---- Demo hints (visible only in local mode) ---------------
                if (statusTextExternal == null) {
                    Text(
                        "Demo režīms: šis ekrāns darbojas arī bez VM — pogas maina lokālu stāvokli.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// 5. ---- Sub-Components --------------------------------------------------------
@Composable
private fun StatusCard(title: String = "Statuss", text: String = "Gatavs.") {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun RecognizedCard(header: String = "Daļējais (partial)", text: String = "—") {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                header,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// 6. ---- Previews --------------------------------------------------------------
@Preview(name = "Elza Screen (MaterialTheme)", showBackground = true, showSystemUi = true)
@Composable
private fun ElzaScreenPreview() {
    // Use default Material3 theme for preview to avoid dependency on project theme.
    MaterialTheme {
        ElzaScreen()
    }
}
