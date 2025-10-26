package lv.mariozo.homeogo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import lv.mariozo.homeogo.ui.ElzaScreen
import lv.mariozo.homeogo.ui.ElzaScreenState
import lv.mariozo.homeogo.ui.InteractionMode
import lv.mariozo.homeogo.ui.SettingsScreen
import lv.mariozo.homeogo.ui.SettingsViewModel
import lv.mariozo.homeogo.ui.theme.HomeoGOTheme
import lv.mariozo.homeogo.ui.viewmodel.ElzaViewModel
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val nav = rememberNavController()

            // Koplietojamais Settings VM (viens un tas pats visai aktivitātei)
            val settingsVm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
            val settingsUi by settingsVm.ui.collectAsState()

            // Elza VM + atļauja
            val elzaVm: ElzaViewModel = viewModel()
            val ui by elzaVm.ui.collectAsState()

            var hasMicPermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        this, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }
            val micPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                hasMicPermission = granted
                if (granted) elzaVm.startListening()
            }

            HomeoGOTheme(useDarkTheme = settingsUi.darkTheme) {
                NavHost(navController = nav, startDestination = "elza") {

                    composable("elza") {
                        val screenState = ElzaScreenState(
                            status = if (hasMicPermission) ui.status else "Nepieciešama mikrofona atļauja",
                            isListening = ui.isListening,
                            messages = emptyList(),
                            speakingMessage = null,
                            interactionMode = InteractionMode.CHAT
                        )
                        ElzaScreen(
                            state = screenState,
                            onStartListening = {
                                if (hasMicPermission) {
                                    elzaVm.startListening()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onStopListening = { elzaVm.stopListening() },
                            onToggleMute = { elzaVm.stopSpeaking() },
                            onModeSelected = { mode ->
                                if (mode == InteractionMode.SETTINGS) {
                                    nav.navigate("settings")
                                }
                            }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            vm = settingsVm,
                            onClose = { nav.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
