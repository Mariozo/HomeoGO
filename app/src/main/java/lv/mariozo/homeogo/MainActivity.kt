//
// ============================================================================
// File: app/src/main/java/lv/mariozo/homeogo/MainActivity.kt
// Module: HomeoGO
// Purpose: Entry point – hosts ElzaScreen (Compose)
// Notes: remove legacy SpeechRecognizerManager
// / Created: 17.sep.2025 21:50
// ver. 1.5
// ============================================================================

package lv.mariozo.homeogo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import lv.mariozo.homeogo.ui.ElzaScreen
import lv.mariozo.homeogo.ui.ElzaScreenState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

// --- patch start: MainActivity.kt (setContent call site) ----------------------
// Replace the existing ElzaScreen() call with this block
        setContent {
            // Temporary bridge to satisfy new required parameters.
            // Later you can wire a real ViewModel and pass its state + callbacks.
            ElzaScreen(
                state = ElzaScreenState(),
                onStartListening = { /* TODO: vm.startListening() */ },
                onStopListening = { /* TODO: vm.stopListening()  */ },
                onSpeakTest = { /* TODO: vm.speakTest(it)    */ }
            )
        }
// --- patch end ----------------------------------------------------------------
    }
}
