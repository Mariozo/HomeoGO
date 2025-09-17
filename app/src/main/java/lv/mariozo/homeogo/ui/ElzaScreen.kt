// File: java/lv/mariozo/homeogo/ui/ElzaScreen.kt
// Module: HomeoGO
// Purpose: Compose UI screen for Elza (STT + TTS skeleton).
// Created: 17.sep.2025 23:15
// ver. 1.0

package lv.mariozo.homeogo.ui

// #1. ---- Imports ---------------------------------------------------
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import lv.mariozo.homeogo.ui.viewmodel.ElzaViewModel // Changed this import to reflect new ViewModel package

/**
 * Pure UI layer (Compose). It observes the ViewModel state and triggers actions.
 * No direct SpeechRecognizer or TTS code here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElzaScreen(vm: ElzaViewModel) { // Changed this line
    val status by vm.status.collectAsState()
    val recognizedText by vm.recognizedText.collectAsState()

    // Ask microphone permission once
    var askedPermission by remember { mutableStateOf(false) }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) vm.setStatus("Mikrofona atļauja liegta")
    }

    LaunchedEffect(Unit) {
        if (!askedPermission) {
            askedPermission = true
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Elza — STT + TTS skelets") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = recognizedText,
                onValueChange = { /* read-only; keep from STT */ },
                label = { Text("Atpazītais teksts") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { vm.startListening() },
                    enabled = !vm.isListening
                ) { Text("Runā") }

                OutlinedButton(
                    onClick = { vm.stopListening() },
                    enabled = vm.isListening
                ) { Text("Stop") }
            }

            HorizontalDivider() // Changed this line

            Button(
                onClick = { vm.speakReply() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Nolasīt atbildi (TTS)") }

            Text(
                "Piezīme: vietā, kur veidojam atbildi, pievieno LLM zvanu (serveris/SDK) un nodod rezultātu uz TTS.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}
