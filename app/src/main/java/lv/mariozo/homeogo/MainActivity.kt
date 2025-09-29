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
import androidx.compose.material3.Surface
import lv.mariozo.homeogo.ui.ElzaScreen
import lv.mariozo.homeogo.ui.theme.HomeoGOTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HomeoGOTheme {
                Surface {
                    ElzaScreen()
                }
            }
        }
    }
}
