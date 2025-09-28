// File: app/src/main/java/lv/mariozo/homeogo/ui/ElzaScreen.kt
// Module: HomeoGO
// Purpose: Pilnais Elzas UI (20.09) + jaunā TTS arhitektūra (Everita/Azure)
// Created: 28.sep.2025 9:15
// ver. 2.0
// Purpose: Elsa UI with settings gear, voice selection (Azure/System),
//          speech rate slider and status area. Works with TtsRouter.
// Notes:
//   - UI: zobrats (iestatījumi), balss dzinēja izvēle (Elza/System),
//          runas ātruma slīdnis, tēmas izvēle, mic/stop + “Pārbaudi balsi”.
//   - TTS: TtsRouter + SystemTtsEngine + AzureTtsEngine (BuildConfig atslēgas).
//   - Runas ātrums pagaidām UI līmenī; Azure piesaiste ar SSML – nākamais solis.
// ============================================================================

package lv.mariozo.homeogo.ui

// #1 Imports -----------------------------------------------------------------
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
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

// #2 Modeļi & Saver ----------------------------------------------------------

enum class TtsChoice { Elza, System }
enum class ThemeChoice { System, Light, Dark }

data class ElzaSettings(
    val ttsChoice: TtsChoice = TtsChoice.Elza,
    val themeChoice: ThemeChoice = ThemeChoice.System,
    val speechRate: Float = 1.0f, // 0.5x..1.5x (UI līmenī; piesaiste Azure – vēlāk)
)

// Saglabāšana rotācijām u.c.
fun settingsSaver() = listSaver<ElzaSettings, Any>(
    save = { listOf(it.ttsChoice.name, it.themeChoice.name, it.speechRate) },
    restore = {
        ElzaSettings(
            ttsChoice = runCatching { TtsChoice.valueOf(it[0] as String) }.getOrDefault(TtsChoice.Elza),
            themeChoice = runCatching { ThemeChoice.valueOf(it[1] as String) }.getOrDefault(
                ThemeChoice.System
            ),
            speechRate = (it[2] as Number).toFloat()
        )
    }
)

// #3 Iestatījumu dialogs -----------------------------------------------------

@Composable
fun SettingsDialog(
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
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // 3.1 Balss dzinējs (Elza/Azure vai Sistēmas TTS)
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
                            onDismissRequest = { ttsMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Elza (Azure)") },
                                onClick = {
                                    tmp = tmp.copy(ttsChoice = TtsChoice.Elza); ttsMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sistēmas TTS") },
                                onClick = {
                                    tmp = tmp.copy(ttsChoice = TtsChoice.System); ttsMenuOpen =
                                    false
                                }
                            )
                        }
                    }
                }

                // 3.2 Runas ātrums (UI līmenī; Azure SSML – nākamais solis)
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

                // 3.3 Tēma (System/Light/Dark) – Theme piesaiste app līmenī, ja vajag
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
                            onDismissRequest = { themeMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Sistēmas") },
                                onClick = {
                                    tmp =
                                        tmp.copy(themeChoice = ThemeChoice.System); themeMenuOpen =
                                    false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Gaišā") },
                                onClick = {
                                    tmp = tmp.copy(themeChoice = ThemeChoice.Light); themeMenuOpen =
                                    false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tumšā") },
                                onClick = {
                                    tmp = tmp.copy(themeChoice = ThemeChoice.Dark); themeMenuOpen =
                                    false
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}

// #4 Galvenais ekrāns --------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElzaScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 4.1 Stāvokļi
    var status by remember { mutableStateOf("Gatava. Piespied “Pārbaudi balsi”.") }
    var showSettings by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) } // STT UI indikators (nākamais solis)

    // 4.2 Iestatījumi (saglabājami)
    var settings by rememberSaveable(stateSaver = settingsSaver()) {
        mutableStateOf(ElzaSettings())
    }

    // 4.3 “Preferred” dzinējs no iestatījumiem
    val preferred = when (settings.ttsChoice) {
        TtsChoice.Elza -> "AzureTTS"
        TtsChoice.System -> "SystemTTS"
    }

    // 4.4 TTS Router (Azure + System) — atjaunojas, kad mainās preferred
    val router = remember(context, preferred) {
        TtsRouter(
            engines = listOf(
                SystemTtsEngine(context),
                AzureTtsEngine(
                    context = context,
                    key = BuildConfig.AZURE_SPEECH_KEY,
                    region = BuildConfig.AZURE_SPEECH_REGION
                )
            ),
            preferred = preferred
        )
    }

    // 4.5 UI karkass
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Elza") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Iestatījumi")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 4.6 Debug info (tikai DEBUG)
            if (BuildConfig.DEBUG) {
                Text(
                    text = "DBG: region='${BuildConfig.AZURE_SPEECH_REGION}', key.len=${BuildConfig.AZURE_SPEECH_KEY.length}",
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.height(8.dp))
            }

            // 4.7 Statusa karte
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Status: $status",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start
                )
            }

            Spacer(Modifier.height(20.dp))

            // 4.8 Vadības pogas – mic/stop (STT UI) + test runa
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        // STT ieslēgšana – nākamais solis pievienosim faktisko klausīšanos
                        isListening = true
                        status = "Klausos… (STT nākamajā solī)"
                    }
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Klausos")
                    Spacer(Modifier.width(8.dp))
                    Text("Klausos")
                }

                OutlinedButton(
                    onClick = {
                        // STT izslēgšana – nākamais solis pievienosim faktiskās klausīšanās stop
                        isListening = false
                        status = "Apturēts."
                    }
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = "Stop")
                    Spacer(Modifier.width(8.dp))
                    Text("Stop")
                }

                Button(
                    onClick = {
                        scope.launch {
                            val phrase = "Sveiki! Te runā Elza Everita."
                            val res = router.speak(phrase) // ← Azure/System atkarībā no iestatījuma
                            status = res.fold(
                                onSuccess = { "OK" },
                                onFailure = { "KĻŪDA: ${it.message ?: "nezināma"}" }
                            )
                        }
                    }
                ) {
                    Text("Pārbaudi balsi")
                }
            }
        }
    }

    // 4.9 Iestatījumu dialogs
    if (showSettings) {
        SettingsDialog(
            initial = settings,
            onDismiss = { showSettings = false },
            onApply = { newSettings ->
                settings = newSettings
                Toast
                    .makeText(
                        context,
                        "Iestatījumi saglabāti: ${newSettings.themeChoice}",
                        Toast.LENGTH_SHORT
                    )
                    .show()
                showSettings = false
            }
        )
    }
}

// #5 Preview (neobligāts) ----------------------------------------------------

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ElzaScreenPreview() {
    MaterialTheme { ElzaScreen() }
}
