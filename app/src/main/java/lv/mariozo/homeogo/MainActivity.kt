// File: app/src/main/java/lv/mariozo/homeogo/MainActivity.kt
// Module: HomeoGO
// Purpose: Main activity entry point showing ElzaScreen (STT/TTS) as primary UI
// Created: 17.sep.2025 21:50 (Europe/Riga)
// ver. 1.4

package lv.mariozo.homeogo

// # --- 1 ------- Imports --------------------------------------------------------
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import lv.mariozo.homeogo.speech.SpeechRecognizerManager
import lv.mariozo.homeogo.ui.ElzaScreen
import lv.mariozo.homeogo.ui.theme.HomeoGOTheme

// # --- 2 ------- MainActivity ---------------------------------------------------
class MainActivity : ComponentActivity() {

    private lateinit var srm: SpeechRecognizerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STT manager (lv-LV)
        srm = SpeechRecognizerManager(this)

        setContent {
            HomeoGOTheme {
                Surface { ElzaScreen() }
            }
        }
    }
}