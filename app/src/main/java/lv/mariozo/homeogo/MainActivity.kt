// # 1.  ------ Header ----------------------------------------------------------
// File: java/lv/mariozo/homeogo/MainActivity.kt
// Module: HomeoGO
// Purpose: Compose host + AndroidX SplashScreen + edge-to-edge
// Created: 17.sep.2025 (Europe/Riga)
// ver. 1.3 - Refactored vm to viewModel
// -----------------------------------------------------------------------------

package lv.mariozo.homeogo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel // Added import for viewModel()
import lv.mariozo.homeogo.ui.ElzaScreen
import lv.mariozo.homeogo.ui.theme.HomeoGOTheme
import lv.mariozo.homeogo.ui.viewmodel.ElzaViewModel // Added import for ElzaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // AndroidX Core SplashScreen (Android 12+ native + backport uz vecākām)
        installSplashScreen()

        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            HomeoGOTheme {
                // Obtain an instance of ElzaViewModel
                val elzaViewModel: ElzaViewModel = viewModel()
                // Pass the ViewModel instance as 'viewModel'
                ElzaScreen(viewModel = elzaViewModel)
            }
        }
    }
}
