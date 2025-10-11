// File: app/src/main/java/lv/mariozo/homeogo/MainActivity.kt
// Project: HomeoGO
// Created: 17.okt.2025 - 11:15 (Europe/Riga)
// ver. 4.6 (SPS-14: Fix incomplete import path)
// Purpose: Host Activity, wired up to the final ViewModel.
// Author: Gemini Agent (Burtnieks & Elza Assistant)
// Comments:
//  - Corrected the incomplete import for SettingsRepository.

package lv.mariozo.homeogo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lv.mariozo.homeogo.logic.SettingsRepository
import lv.mariozo.homeogo.ui.ElzaScreen
import lv.mariozo.homeogo.ui.ElzaViewModel
import lv.mariozo.homeogo.ui.InteractionMode
import lv.mariozo.homeogo.ui.SettingsScreen
import lv.mariozo.homeogo.ui.SettingsViewModel
import lv.mariozo.homeogo.ui.theme.HomeoGOTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val vm: ElzaViewModel = viewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()

            val settingsVM: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
            // Use collectAsState and explicitly import the type to fix inference
            val settings: SettingsRepository.UiSnapshot by settingsVM.ui.collectAsState()

            LaunchedEffect(settings.enableBargeIn) {
                vm.setBargeInEnabled(settings.enableBargeIn)
            }

            var showSettingsScreen by remember { mutableStateOf(false) }

            val micPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { granted ->
                    if (granted) vm.startListening() else vm.onPermissionDenied()
                }
            )
            val requestMicThenStart = {
                val granted = ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) vm.startListening() else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }

            HomeoGOTheme(useDarkTheme = settings.darkTheme) {
                if (showSettingsScreen) {
                    SettingsScreen(
                        vm = settingsVM,
                        onClose = { showSettingsScreen = false }
                    )
                } else {
                    ElzaScreen(
                        state = state,
                        onStartListening = { requestMicThenStart() },
                        onStopListening = { vm.stopListening() },
                        onToggleMute = { vm.toggleMuteMode() },
                        onModeSelected = {
                            when (it) {
                                InteractionMode.SETTINGS -> showSettingsScreen = true
                                else -> vm.setInteractionMode(it)
                            }
                        }
                    )
                }
            }
        }
    }
}
