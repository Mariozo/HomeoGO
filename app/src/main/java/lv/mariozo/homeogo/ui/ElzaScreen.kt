// File: java/lv/mariozo/homeogo/ui/ElzaScreen.kt
// Module: HomeoGO
// Purpose: Compose UI screen for Elza (STT + TTS skeleton).
// Created: 17.sep.2025 23:15
// ver. 1.1 - Adapted to use ElzaViewModel's uiState

package lv.mariozo.homeogo.ui

// #1. ---- Imports ---------------------------------------------------
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import lv.mariozo.homeogo.ui.viewmodel.ElzaViewModel

/**
 * Pure UI layer (Compose). It observes the ViewModel state and triggers actions.
 * No direct SpeechRecognizer or TTS code here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElzaScreen(vm: ElzaViewModel) {
    val uiState by vm.uiState.collectAsState()

    // Ask microphone permission once
    var askedPermission by remember { mutableStateOf(false) }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) vm.reportPermissionDenied() // Updated call
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
                text = uiState.status, // Updated access
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = uiState.recognizedText, // Updated access
                onValueChange = { /* read-only; value comes from ViewModel */ },
                label = { Text("Atpazītais teksts") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { vm.startListening() },
                    enabled = !uiState.isListening // Updated access
                ) { Text("Runā") }

                OutlinedButton(
                    onClick = { vm.stopListening() },
                    enabled = uiState.isListening // Updated access
                ) { Text("Stop") }
            }

            HorizontalDivider()

            Button(
                onClick = { vm.speak(uiState.recognizedText) }, // Updated call
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.recognizedText.isNotBlank() // Enable only if there's text
            ) { Text("Nolasīt atbildi (TTS)") }

            Text(
                "Piezīme: vietā, kur veidojam atbildi, pievieno LLM zvanu (serveris/SDK) un nodod rezultātu uz TTS.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}
