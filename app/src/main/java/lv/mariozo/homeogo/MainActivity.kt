// File: app/src/main/java/lv/mariozo/homeogo/MainActivity.kt
// Project: HomeoGO
// Created: 03.okt.2025 12:05 (Rīga)
// ver. 1.3
// Purpose: Host activity for HomeoGO. Provides ElzaViewModel to ElzaScreen,
//          wiring UI state and STT/TTS callbacks.
// Comments:
//  - Uses lifecycle-viewmodel-compose + lifecycle-runtime-compose for state handling.
//  - Theme defined in themes.xml (Theme.HomeoGO).

package lv.mariozo.homeogo

// 1. ---- Imports ---------------------------------------------------------------
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lv.mariozo.homeogo.ui.ElzaScreen
import lv.mariozo.homeogo.ui.ElzaViewModel

// 2. ---- Activity --------------------------------------------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val vm: ElzaViewModel = viewModel()
            val state = vm.uiState.collectAsStateWithLifecycle().value

            MaterialTheme {
                ElzaScreen(
                    state = state,
                    onStartListening = { vm.startListening() },
                    onStopListening = { vm.stopListening() },
                    onSpeakTest = { vm.speakTest(it) }
                )
            }
        }
    }
}
