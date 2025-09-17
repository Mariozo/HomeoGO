

// File: java/lv/mariozo/homeogo/MainActivity.kt
// Module: HomeoGO
// Purpose: Compose host + AndroidX SplashScreen (keep-on-screen + exit animation) + edge-to-edge
// Created: 17.sep.2025 21:50 (Europe/Riga)
// ver. 1.4


package lv.mariozo.homeogo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import lv.mariozo.homeogo.ui.ElzaScreen
import lv.mariozo.homeogo.ui.theme.HomeoGOTheme
import lv.mariozo.homeogo.ui.viewmodel.ElzaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // AndroidX Core SplashScreen (Android 12+ native + backport uz vecākām)
        val splash = installSplashScreen()

        // Īss, drošs aizturējums (≈250 ms), lai Compose pirmais kadrs gatavs
        var keepOnScreen = true
        splash.setKeepOnScreenCondition { keepOnScreen }

        // Vienkārša izejas animācija (ikonas izbalināšana)
        splash.setOnExitAnimationListener { provider ->
            provider.iconView
                .animate()
                .alpha(0f)
                .setDuration(200L)
                .withEndAction { provider.remove() }
                .start()
        }

        // Atlaid splash pēc īsa brīža (bez korutīnām, izmanto Handler)
        Handler(Looper.getMainLooper()).postDelayed(
            { keepOnScreen = false },
            250L
        )

        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val viewModel = ViewModelProvider(this)[ElzaViewModel::class.java]

        setContent {
            HomeoGOTheme {
                // Galvenais ekrāns — Elza (STT/TTS)
                ElzaScreen(viewModel = viewModel)
            }
        }
    }
}
