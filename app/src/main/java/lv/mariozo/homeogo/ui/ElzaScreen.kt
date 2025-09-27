// File: app/src/main/java/lv/mariozo/homeogo/ui/ElzaScreen.kt
// Module: HomeoGO
// Purpose: Compose UI for Elza (STT + TTS test + Settings dialog + Previews)
// Created: 19.sep.2025 23:10
// ver. 1.7

// ============================================================================
// File: app/src/main/java/lv/mariozo/homeogo/ui/ElzaScreen.kt
// Module: HomeoGO
// Purpose: Elza screen with TTS router (System + Azure engines) and test button
// Created: 27.sep.2025 15:40 (Europe/Riga)
// ver. 1.8
// Notes:
//   - Requires: TtsRouter, SystemTtsEngine, AzureTtsEngine
//   - BuildConfig must contain AZURE_SPEECH_KEY and AZURE_SPEECH_REGION
//     (uses TtsRouter + engines; shows status)
// ============================================================================

package lv.mariozo.homeogo.ui

// # --- 1 ------- Imports -----------------------------------------------------
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import lv.mariozo.homeogo.BuildConfig
import lv.mariozo.homeogo.voice.TtsRouter
import lv.mariozo.homeogo.voice.tts.azure.AzureTtsEngine
import lv.mariozo.homeogo.voice.tts.system.SystemTtsEngine

// # --- 2 ------- Public Composable ------------------------------------------
@Composable
fun ElzaScreen(
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("—") }

    // Router ar diviem dzinējiem (System + Azure)
    val router = remember {
        TtsRouter(
            engines = listOf(
                SystemTtsEngine(ctx),
                AzureTtsEngine(
                    context = ctx,
                    key = BuildConfig.AZURE_SPEECH_KEY,
                    region = BuildConfig.AZURE_SPEECH_REGION
                )
            ),
            preferred = "AzureTTS" // var mainīt uz "SystemTTS"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Elza",
            style = MaterialTheme.typography.headlineMedium
        )

        // Debug rindiņas (uz brīdi, lai pārliecinātos par BuildConfig)
        Text("DBG region='${BuildConfig.AZURE_SPEECH_REGION}' key.len=${BuildConfig.AZURE_SPEECH_KEY.length}")

        Surface(
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Statuss: $status",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start
            )
        }

        Button(
            onClick = {
                scope.launch {
                    val res = router.speak("Sveiki! Te runā Everita.")
                    status = res.fold(
                        onSuccess = { "OK" },
                        onFailure = { "ERROR: ${it.message}" }
                    )
                }
            }
        ) {
            Text("Pārbaudi balsi")
        }
    }
}

// # --- 3 ------- Preview -----------------------------------------------------
@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
private fun ElzaScreenPreview() {
    MaterialTheme {
        ElzaScreen()
    }
}
