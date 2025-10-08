// File: app/src/main/java/lv/mariozo/homeogo/MainActivity.kt
// Project: HomeoGO
// Created: 11.okt.2025 (Rīga)
// ver. 4.0 (FINAL - Aligned with new ViewModel)
// Purpose: Host Activity, wired up to the final ViewModel.

package lv.mariozo.homeogo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lv.mariozo.homeogo.ui.ElzaScreen
import lv.mariozo.homeogo.ui.ElzaViewModel
import lv.mariozo.homeogo.ui.theme.HomeoGOTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        Log.d(
            "HomeoGO-API",
            "BASE=${BuildConfig.ELZA_API_BASE} PATH=${BuildConfig.ELZA_API_PATH} TOKEN_EMPTY=${BuildConfig.ELZA_API_TOKEN.isEmpty()}"
        )

        setContent {
            val vm: ElzaViewModel = viewModel()
            val state = vm.uiState.collectAsStateWithLifecycle().value

            val micPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { granted ->
                    if (granted) {
                        vm.startListening()
                    } else {
                        vm.onPermissionDenied()
                    }
                }
            )

            fun requestMicThenStart() {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    vm.startListening()
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            HomeoGOTheme {
                ElzaScreen(
                    state = state,
                    onStartListening = { requestMicThenStart() },
                    onStopListening = { vm.stopListening() },
                    onToggleMute = { vm.toggleMuteMode() }
                )
            }
        }
    }
}