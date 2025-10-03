// File: app/src/main/java/lv/mariozo/homeogo/MainActivity.kt
// Project: HomeoGO (Android, Jetpack Compose + Material3)
// Created: 03.okt.2025 07:55 (Rīga)
// ver. 1.2
// Purpose: Host activity providing ElzaViewModel to ElzaScreen; wires VM state
//          and callbacks to UI. Entry point for HomeoGO app.
// Comments:
//  - Uses lifecycle-viewmodel-compose for state collection.
//  - Replace viewModel() with hiltViewModel() if project uses Hilt.
//  - Initializes SpeechRecognizerManager and TtsManager in VM factory.

package lv.mariozo.homeogo

// 1. ---- Imports ---------------------------------------------------------------
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lv.mariozo.homeogo.ui.ElzaScreen
import lv.mariozo.homeogo.ui.ElzaViewModel
import lv.mariozo.homeogo.ui.theme.HomeoGOTheme
import lv.mariozo.homeogo.voice.SpeechRecognizerManager
import lv.mariozo.homeogo.voice.TtsManager

// 2. ---- Activity --------------------------------------------------------------
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // 2.1 ---- VM provider with managers --------------------------------
            val vm: ElzaViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        val stt = SpeechRecognizerManager(applicationContext)
                        val tts = TtsManager(applicationContext)
                        return ElzaViewModel(stt, tts) as T
                    }
                }
            )

            // 2.2 ---- Observe UI state ----------------------------------------
            val state = vm.uiState.collectAsStateWithLifecycle().value

            // 2.3 ---- Compose UI -----------------------------------------------
            HomeoGOTheme {
                ElzaScreen(
                    state = state,
                    onStartListening = { vm.startListening() },
                    onStopListening = { vm.stopListening() },
                    onSpeakTest = { txt -> vm.speakTest(txt) }
                )
            }
        }
    }
}
