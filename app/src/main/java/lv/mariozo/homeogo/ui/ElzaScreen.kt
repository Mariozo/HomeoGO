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

// File: app/src/main/java/lv/mariozo/homeogo/ui/ElzaScreen.kt
// Project: HomeoGO (Android, Jetpack Compose + Material3)
// Module: app
// Purpose: Visual Elza screen wired for real operation (no local demo state). UI state comes
//          from ViewModel; actions are forwarded via callbacks to STT/TTS managers.
// Created: 01.okt.2025 17:47
// ver. 1.9
// Notes:
//  - UI layout/logic preserved; only state source changed to external VM-provided state.
//  - No direct references/imports to Azure/STT classes; integration via callbacks.
//  - Two previews with mock state keep AS Preview working without project dependencies.
//  - UI strings LV; code/comments EN. Blocks follow MK! numbering.


@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package lv.mariozo.homeogo.ui
// 1. ---- Imports ---------------------------------------------------------------
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// 2. ---- Public state & API (from ViewModel) ----------------------------------
// The ViewModel should expose this shape (directly or via mapping).
data class ElzaScreenState(
    val isListening: Boolean = false,
    val status: String = "Gatavs.",
    val partialText: String = "",
    val finalText: String = "",
    val testPhrase: String = "Sveiki! Šis ir Elzas testa teikums.",
)

// Host screen should pass real callbacks that call VM -> STT/TTS managers:
//   onStartListening() -> vm.startListening()
//   onStopListening()  -> vm.stopListening()
//   onSpeakTest(txt)   -> vm.speakTest(txt)
@Composable
fun ElzaScreen(
    state: ElzaScreenState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSpeakTest: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Local edit buffer for test phrase (does not drive app logic).
    var testPhraseBuffer by rememberSaveable(state.testPhrase) { mutableStateOf(state.testPhrase) }

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
                // 4.1 ---- Status block -----------------------------------------
                StatusBlock(
                    status = state.status,
                    isListening = state.isListening,
                    partial = state.partialText,
                    finalText = state.finalText
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
                            onStartListening()
                        },
                        enabled = !state.isListening
                    ) { Text("Klausos") }

                    OutlinedButton(
                        onClick = {
                            onStopListening()
                        },
                        enabled = state.isListening
                    ) { Text("Stop") }

                    Button(
                        onClick = {
                            val toSpeak = state.finalText.ifBlank { testPhraseBuffer }
                            onSpeakTest(toSpeak)
                        }
                    ) { Text("Pārbaudīt balsi") }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // 4.3 ---- Recognized text panels --------------------------------
                RecognizedCard(
                    header = "Daļējais (partial)",
                    text = state.partialText.ifBlank { "—" })
                Spacer(Modifier.height(12.dp))
                RecognizedCard(header = "Gala (final)", text = state.finalText.ifBlank { "—" })

                Spacer(Modifier.height(24.dp))

                // 4.4 ---- Test phrase input (for TTS) ---------------------------
                Text(
                    "Tests frāzei (TTS):",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = testPhraseBuffer,
                    onValueChange = { testPhraseBuffer = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ieraksti ko Elza nolasīs…") },
                    singleLine = true
                )
            }
        }
    }
}

// 5. ---- Sub-Components --------------------------------------------------------
@Composable
private fun StatusBlock(status: String, isListening: Boolean, partial: String, finalText: String) {
    val cfg = LocalConfiguration.current
    val isLandscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE

    val dynamicText =
        if (isListening && partial.isNotBlank()) "… $partial"
        else if (isListening) "Klausos… runā tagad."
        else if (finalText.isNotBlank()) finalText
        else status.ifBlank { "Gatava. Piespied “Klausos”." }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) 96.dp else 148.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = dynamicText,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun RecognizedCard(header: String, text: String) {
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

// 6. ---- Previews (mock state, no VM required) --------------------------------
// --- patch start: ElzaScreen.kt (previews — split & public) -------------------
@Preview(
    name = "Elza — Light",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun ElzaPreview_Light() {
    val mock = ElzaScreenState(
        isListening = true,
        status = "Klausos…",
        partialText = "Sveiki, šis ir daļējs…",
        finalText = ""
    )
    MaterialTheme {
        ElzaScreen(
            state = mock,
            onStartListening = {},
            onStopListening = {},
            onSpeakTest = {}
        )
    }
}

@Preview(
    name = "Elza — Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun ElzaPreview_Dark() {
    val mock = ElzaScreenState(
        isListening = false,
        status = "Gatavs.",
        partialText = "",
        finalText = "Gala teksts priekšskatījumam."
    )
    MaterialTheme {
        ElzaScreen(
            state = mock,
            onStartListening = {},
            onStopListening = {},
            onSpeakTest = {}
        )
    }
}
// --- patch end ----------------------------------------------------------------

