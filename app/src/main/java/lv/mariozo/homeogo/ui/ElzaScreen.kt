// File: app/src/main/java/lv/mariozo/homeogo/ui/ElzaScreen.kt
// Project: HomeoGO
// Created: 03.okt.2025 12:10 (Rīga)
// ver. 1.4
// Purpose: Compose UI screen for Elza assistant. Shows STT status, recognized text,
//          and provides buttons for start/stop listening and TTS test.
// Comments:
//  - Uses ElzaScreenState (from ViewModel).
//  - Preview uses mock state (no managers).
//  - UI texts in LV; code/comments in EN.

package lv.mariozo.homeogo.ui

// 1. ---- Imports ---------------------------------------------------------------
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// 2. ---- Screen Composable -----------------------------------------------------
@Composable
fun ElzaScreen(
    state: ElzaScreenState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSpeakTest: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Stāvoklis: ${state.status}", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Atpazītais teksts: ${state.recognizedText}",
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider(thickness = 1.dp)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStartListening, enabled = !state.isListening) {
                    Text("Klausos")
                }
                Button(onClick = onStopListening, enabled = state.isListening) {
                    Text("Stop")
                }
            }

            Button(onClick = { onSpeakTest("Sveika, Elza!") }) {
                Text("Pārbaudīt balsi")
            }
        }
    }
}

// 3. ---- Preview (Light/Dark) -------------------------------------------------
@Preview(name = "Elza Preview Light", showBackground = true, showSystemUi = true)
@Composable
fun ElzaPreview_Light() {
    MaterialTheme {
        ElzaScreen(
            state = ElzaScreenState(
                status = "Idle",
                recognizedText = "Paraugs",
                isListening = false
            ),
            onStartListening = {},
            onStopListening = {},
            onSpeakTest = {}
        )
    }
}

@Preview(
    name = "Elza Preview Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true, showSystemUi = true
)
@Composable
fun ElzaPreview_Dark() {
    MaterialTheme {
        ElzaScreen(
            state = ElzaScreenState(
                status = "Klausos...",
                recognizedText = "Paraugs tumšajā režīmā",
                isListening = true
            ),
            onStartListening = {},
            onStopListening = {},
            onSpeakTest = {}
        )
    }
}
