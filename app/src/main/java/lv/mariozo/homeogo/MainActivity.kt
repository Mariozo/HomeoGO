// File: app/src/main/java/lv/mariozo/homeogo/MainActivity.kt
// Project: HomeoGO
// Created: 03.okt.2025 12:50 (Rīga)
// ver. 1.6
// Purpose: Host Activity. Enables edge-to-edge display and handles mic permissions.
// Comments:
//  - Added enableEdgeToEdge() for modern UI behavior.

package lv.mariozo.homeogo

// 1. ---- Imports ---------------------------------------------------------------
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
import lv.mariozo.homeogo.BuildConfig as AppBuildConfig


// 2. ---- Activity --------------------------------------------------------------
class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable drawing behind system bars
        enableEdgeToEdge()

        Log.d(
            "HomeoGO-API",
            "BASE=${AppBuildConfig.ELZA_API_BASE} PATH=${AppBuildConfig.ELZA_API_PATH} TOKEN_EMPTY=${AppBuildConfig.ELZA_API_TOKEN.isEmpty()}"
        )

        setContent {
            // Use the default ViewModel provider. It will correctly call the
            // ElzaViewModel(application: Application) constructor.
            val vm: ElzaViewModel = viewModel()
            val state = vm.uiState.collectAsStateWithLifecycle().value

            // Launcher to request microphone permission and start STT if granted
            val micPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    vm.startListening()
                } else {
                    // If the user denies permission, update the status via the VM
                    vm.onPermissionDenied()
                }
            }

            // Helper function to check for permission and either start STT or request permission
            fun requestMicThenStart() {
                val isGranted = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (isGranted) {
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
                    onClearChat = { vm.clearChat() }
                )
            }
        }
    }
}
