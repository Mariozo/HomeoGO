// File: app/src/main/java/lv/mariozo/homeogo/ui/ElzaScreen.kt
// Module: HomeoGO
// Purpose: Compose UI for Elza (STT + TTS test + Settings dialog + Previews)
// Created: 19.sep.2025 23:10
// ver. 1.7

package lv.mariozo.homeogo.ui

// # 1. ------- Imports -----------------------------------------------------------
import android.Manifest
import android.app.Activity
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import lv.mariozo.homeogo.speech.SpeechRecognizerManager
import lv.mariozo.homeogo.voice.TTSManager
import androidx.compose.material3.ExperimentalMaterial3Api


// # 2. ------- Modeļi ------------------------------------------------------------
enum class TtsChoice { System, Elza }
enum class ThemeChoice { System, Light, Dark }

data class ElzaSettings(
    val ttsChoice: TtsChoice = TtsChoice.Elza,
    val themeChoice: ThemeChoice = ThemeChoice.System,
    val speakingRate: Float = 1.0f,   // 0.5..1.5 (pagaidām neizmantojam)
    val speakingPitch: Float = 0.0f   // semitoni (pagaidām neizmantojam)
)

sealed interface ElzaUiState {
    data object Idle : ElzaUiState
    data object Listening : ElzaUiState
    data object Processing : ElzaUiState
    data object Speaking : ElzaUiState
    data class Error(val message: String) : ElzaUiState
    data class Partial(val text: String) : ElzaUiState
    data class Final(val text: String) : ElzaUiState
}

// # 3. ------- Ekrāns ------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElzaScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? Activity

    // STT manager
    val srm = androidx.compose.runtime.remember(activity) { activity?.let { SpeechRecognizerManager(it) } }

    // StateFlow → Compose state (bez lifecycle extension)
    val sttState by (srm?.state?.collectAsState(initial = SpeechRecognizerManager.SttState.Idle)
        ?: androidx.compose.runtime.remember { mutableStateOf(SpeechRecognizerManager.SttState.Idle) })

    val uiState = when (sttState) {
        is SpeechRecognizerManager.SttState.Idle      -> ElzaUiState.Idle
        is SpeechRecognizerManager.SttState.Listening -> ElzaUiState.Listening
        is SpeechRecognizerManager.SttState.Partial   -> ElzaUiState.Partial((sttState as SpeechRecognizerManager.SttState.Partial).text)
        is SpeechRecognizerManager.SttState.Final     -> ElzaUiState.Final((sttState as SpeechRecognizerManager.SttState.Final).text)
        is SpeechRecognizerManager.SttState.Error     -> ElzaUiState.Error((sttState as SpeechRecognizerManager.SttState.Error).message)
        else -> ElzaUiState.Idle
    }

    // Mikrofona atļauja
    var hasPermission by rememberSaveable { mutableStateOf(false) }
    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    fun startWithPermission() {
        if (activity == null || srm == null) {
            Toast.makeText(context, "Nav pieejama Activity vide.", Toast.LENGTH_SHORT).show()
            return
        }
        if (hasPermission) srm.startListening()
        else requestPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Stop klausīšanos, kad ekrāns pazūd
    DisposableEffect(srm) { onDispose { srm?.stopListening() } }

    // TTS (šobrīd izmantojam tikai speak)
    val tts = androidx.compose.runtime.remember(context) { TTSManager(context) }
    // Ja tavā TTSManager ir shutdown()/release(), paziņo man — pielikšu atpakaļ.

    var showSettings by rememberSaveable { mutableStateOf(false) }
    var settings by rememberSaveable(stateSaver = settingsSaver()) { mutableStateOf(ElzaSettings()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Elza", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Iestatījumi")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Statusa karte
            StatusBlock(uiState)

            // Pamatpogas
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val listening = uiState is ElzaUiState.Listening
                val speaking = uiState is ElzaUiState.Speaking

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        enabled = !listening,
                        onClick = { startWithPermission() },
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Klausos")
                    }
                    OutlinedButton(
                        onClick = { srm?.stopListening() },
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Outlined.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Stop")
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        // Pagaidām tikai runā testa frāzi.
                        tts.speak("Sveiki! Šis ir Elzas testa teikums latviešu valodā.")
                    },
                    enabled = !speaking,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("▶️ Pārbaudi balsi")
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            initial = settings,
            onDismiss = { showSettings = false },
            onApply = {
                settings = it
                Toast.makeText(context, "Iestatījumi saglabāti: ${it.themeChoice}", Toast.LENGTH_SHORT).show()
                showSettings = false
            }
        )
    }
}

// # 4. ------- Status bloks ------------------------------------------------------
@Composable
private fun StatusBlock(uiState: ElzaUiState) {
    val text = when (uiState) {
        ElzaUiState.Idle -> "Gatava. Piespied “Klausos”."
        ElzaUiState.Listening -> "Klausos… runā tagad."
        ElzaUiState.Processing -> "Apstrādāju…"
        ElzaUiState.Speaking -> "Atskaņoju Elzas balsi…"
        is ElzaUiState.Partial -> "… ${uiState.text}"
        is ElzaUiState.Final -> uiState.text
        is ElzaUiState.Error -> "Kļūda: ${uiState.message}"
    }

    val cfg = LocalConfiguration.current
    val isLandscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) 96.dp else 160.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

// # 5. ------- Settings dialogs --------------------------------------------------
@Composable
private fun SettingsDialog(
    initial: ElzaSettings,
    onDismiss: () -> Unit,
    onApply: (ElzaSettings) -> Unit
) {
    var ttsChoice by rememberSaveable { mutableStateOf(initial.ttsChoice) }
    var themeChoice by rememberSaveable { mutableStateOf(initial.themeChoice) }
    var rate by rememberSaveable { mutableStateOf(initial.speakingRate) }
    var pitch by rememberSaveable { mutableStateOf(initial.speakingPitch) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onApply(ElzaSettings(ttsChoice, themeChoice, rate, pitch)) }) {
                Text("Labi")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Atcelt") } },
        title = { Text("Iestatījumi", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Balss dzinējs", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = ttsChoice == TtsChoice.Elza, onClick = { ttsChoice = TtsChoice.Elza })
                    Spacer(Modifier.width(6.dp)); Text("Elza (tīkla TTS)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = ttsChoice == TtsChoice.System, onClick = { ttsChoice = TtsChoice.System })
                    Spacer(Modifier.width(6.dp)); Text("Sistēmas TTS")
                }

                Spacer(Modifier.height(8.dp))

                Text("Tēmas režīms", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = themeChoice == ThemeChoice.System, onClick = { themeChoice = ThemeChoice.System })
                    Spacer(Modifier.width(6.dp)); Text("Sistēmas")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = themeChoice == ThemeChoice.Light, onClick = { themeChoice = ThemeChoice.Light })
                    Spacer(Modifier.width(6.dp)); Text("Gaišā")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = themeChoice == ThemeChoice.Dark, onClick = { themeChoice = ThemeChoice.Dark })
                    Spacer(Modifier.width(6.dp)); Text("Tumšā")
                }

                Spacer(Modifier.height(8.dp))

                Text("Balss ātrums", style = MaterialTheme.typography.labelLarge)
                Slider(value = rate, onValueChange = { rate = it }, valueRange = 0.5f..1.5f, steps = 8)
                Text(String.format("x %.2f", rate), style = MaterialTheme.typography.bodySmall)

                Spacer(Modifier.height(4.dp))

                Text("Balss tonis (semitoni)", style = MaterialTheme.typography.labelLarge)
                Slider(value = pitch, onValueChange = { pitch = it }, valueRange = -6f..6f, steps = 11)
                Text(String.format("%+.1f st", pitch), style = MaterialTheme.typography.bodySmall)
            }
        }
    )
}

// # 6. ------- Saver -------------------------------------------------------------
private fun settingsSaver(): Saver<ElzaSettings, Any> = Saver(
    save = { listOf(it.ttsChoice.name, it.themeChoice.name, it.speakingRate, it.speakingPitch) },
    restore = { raw ->
        val l = raw as List<*>
        ElzaSettings(
            ttsChoice = TtsChoice.valueOf(l[0] as String),
            themeChoice = ThemeChoice.valueOf(l[1] as String),
            speakingRate = (l[2] as Number).toFloat(),
            speakingPitch = (l[3] as Number).toFloat()
        )
    }
)

// # 7. ------- Previews ----------------------------------------------------------
@Preview(name = "Elza — Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Elza — Dark",  showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ElzaPreview() {
    Surface { ElzaScreen() }
}
