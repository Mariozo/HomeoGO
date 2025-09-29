//
// ============================================================================
// File: app/src/main/java/lv/mariozo/homeogo/ui/ElzaScreen.kt
// Module: HomeoGO
// Purpose: Pilnais Elzas UI (20.09) + jaunā TTS arhitektūra (Everita/Azure)
// Created: 28.sep.2025 9:15
// ver. 2.0
// Purpose: Elsa UI with settings gear, voice selection (Azure/System),
//          speech rate slider and status area. Works with TtsRouter.
// Notes:
//   - TTS: Azure parametri no BuildConfig.AZURE_SPEECH_KEY/REGION.
//   - STT: vajag RECORD_AUDIO atļauju (Manifest + runtime).
//   - Runas ātrums UI pusē saglabājas; Azure SSML var piesiet nākamajā solī.
// ============================================================================

package lv.mariozo.homeogo.ui

// #1 Imports -----------------------------------------------------------------
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import lv.mariozo.homeogo.BuildConfig
import lv.mariozo.homeogo.voice.TtsRouter
import lv.mariozo.homeogo.voice.tts.azure.AzureTtsEngine
import lv.mariozo.homeogo.voice.tts.system.SystemTtsEngine

// Azure STT SDK
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig

// #2 ---- Modeļi & Saver ---------------------------------

enum class TtsChoice { Elza, System }
enum class ThemeChoice { System, Light, Dark }

data class ElzaSettings(
    val ttsChoice: TtsChoice = TtsChoice.Elza,
    val themeChoice: ThemeChoice = ThemeChoice.System,
    val speechRate: Float = 1.0f, // 0.5x..1.5x
)

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

// #3 ---- Settings dialogue ----------------------------

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

                // Balss dzinējs
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

                // Runas ātrums (UI)
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

                // Tēma
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

// #4 ---- Local Azure STT service ---------------------------

private class LocalAzureSttService(
    context: Context,
    key: String,
    region: String,
    language: String = "lv-LV",
) {
    private val speechConfig: SpeechConfig = SpeechConfig.fromSubscription(key, region).apply {
        speechRecognitionLanguage = language
    }
    private var recognizer: SpeechRecognizer? = null

    fun start(
        onResult: (String) -> Unit,
        onError: (Throwable) -> Unit = {},
    ) {
        stop()
        val audioCfg = AudioConfig.fromDefaultMicrophoneInput()
        recognizer = SpeechRecognizer(speechConfig, audioCfg).apply {
            recognizing.addEventListener { _, _ -> /* starprezultāti, ja vajag */ }
            recognized.addEventListener { _, e ->
                val txt = e.result?.text.orEmpty()
                if (txt.isNotBlank()) onResult(txt)
            }
            canceled.addEventListener { _, e ->
                onError(IllegalStateException("STT canceled: ${e.errorCode} ${e.errorDetails}"))
            }
            sessionStopped.addEventListener { _, _ -> /* no-op */ }
        }
        try {
            recognizer?.startContinuousRecognitionAsync()?.get()
        } catch (t: Throwable) {
            onError(t)
        }
    }

    fun stop() {
        try {
            recognizer?.stopContinuousRecognitionAsync()?.get()
        } catch (_: Throwable) { /* ignore */
        }
        recognizer?.close()
        recognizer = null
    }
}

// #5 ---- Useful helpers Activity ---------------------------

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    else -> (this as? ContextWrapper)?.baseContext?.findActivity()
}

// --------------------------- Galvenais ekrāns -------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElzaScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("Gatava. Piespied “Pārbaudi balsi”.") }
    var showSettings by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }

    var settings by rememberSaveable(stateSaver = settingsSaver()) {
        mutableStateOf(ElzaSettings())
    }

    val preferred = when (settings.ttsChoice) {
        TtsChoice.Elza -> "AzureTTS"
        TtsChoice.System -> "SystemTTS"
    }

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

    // ---------- Runtime RECORD_AUDIO permission ----------
    val recordAudioPerm = Manifest.permission.RECORD_AUDIO
    var hasRecordAudio by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, recordAudioPerm) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasRecordAudio = granted }

    // ---------- STT serviss + dzīvescikls ----------
    val stt = remember {
        LocalAzureSttService(
            context = context,
            key = BuildConfig.AZURE_SPEECH_KEY,
            region = BuildConfig.AZURE_SPEECH_REGION
        )
    }
    DisposableEffect(Unit) { onDispose { stt.stop() } }

    // --------------------------- UI karkass ---------------------------------
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
            if (BuildConfig.DEBUG) {
                Text(
                    text = "DBG: region='${BuildConfig.AZURE_SPEECH_REGION}', key.len=${BuildConfig.AZURE_SPEECH_KEY.length}",
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.height(8.dp))
            }

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

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic
                FilledTonalButton(
                    onClick = {
                        if (!hasRecordAudio) {
                            // Ja vajag, vari pievienot savu paskaidrojuma dialogu
                            permissionLauncher.launch(recordAudioPerm)
                            return@FilledTonalButton
                        }
                        isListening = true
                        status = "Klausos…"
                        stt.start(
                            onResult = { txt ->
                                status = "⟪$txt⟫"
                                scope.launch {
                                    val res = speakWithRate(
                                        router,
                                        preferred,
                                        context,
                                        txt,
                                        settings.speechRate
                                    )
                                    status = res.fold(
                                        onSuccess = { "OK" },
                                        onFailure = { "KĻŪDA: ${it.message ?: "nezināma"}" }
                                    )
                                }
                            },
                            onError = { err ->
                                isListening = false
                                status = "STT kļūda: ${err.message}"
                            }
                        )
                    }
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Klausos")
                    Spacer(Modifier.width(8.dp))
                    Text("Klausos")
                }

                // Stop
                OutlinedButton(
                    onClick = {
                        stt.stop()
                        isListening = false
                        status = "Apturēts."
                    }
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = "Stop")
                    Spacer(Modifier.width(8.dp))
                    Text("Stop")
                }

                // Pārbaudi balsi
                Button(
                    onClick = {
                        scope.launch {
                            val phrase = "Sveiki! Te runā Elza Everita."
                            val res = speakWithRate(
                                router,
                                preferred,
                                context,
                                phrase,
                                settings.speechRate
                            )
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

    if (showSettings) {
        SettingsDialog(
            initial = settings,
            onDismiss = { showSettings = false },
            onApply = { newSettings ->
                settings = newSettings
                Toast.makeText(
                    context,
                    "Iestatījumi saglabāti: ${newSettings.themeChoice}",
                    Toast.LENGTH_SHORT
                ).show()
                showSettings = false
            }
        )
    }
}

// --------------------------- Palīgfunkcija TTS -------------------------------

private suspend fun speakWithRate(
    router: TtsRouter,
    preferred: String,
    context: Context,
    text: String,
    rate: Float,
): Result<Unit> {
    // Šobrīd: ja Azure + rate ≠ 1.0 → varēsim pārslēgt uz azure.speakSsml(text, rate)
    // Kad ieviesīsi SSML AzureTtsEngine, nomaini zemāk uz speakSsml.
    return if (preferred == "AzureTTS" && kotlin.math.abs(rate - 1.0f) >= 0.01f) {
        val azure = AzureTtsEngine(
            context = context,
            key = BuildConfig.AZURE_SPEECH_KEY,
            region = BuildConfig.AZURE_SPEECH_REGION
        )
        azure.speak(text) // TODO: pāriet uz azure.speakSsml(text, rate)
    } else {
        router.speak(text)
    }
}
