// File: app/src/main/java/lv/mariozo/homeogo/MainActivity.kt
// Project: HomeoGO
// Created: 13.okt.2025 - 21:00 (Europe/Riga)
// ver. 5.0 — Simplified: no auto-listen, only mode sync; mic permission gate
// Purpose: Host Activity, wired up to the final ViewModel.

package lv.mariozo.homeogo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import lv.mariozo.homeogo.ui.ElzaScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val vm: ElzaViewModel = viewModel()
            val state by vm.uiState.collectAsState()

            ElzaScreen(
                state = state,
                onStartListening = { vm.startListening() },
                onStopListening = { vm.stopListening() },
                onToggleMute = { /* TODO: implement mute; temporary no-op */ },
                onModeSelected = { mode -> vm.setInteractionMode(mode) }
            )
        }
    }
}
