// File: app/src/main/java/lv/mariozo/homeogo/MainActivity.kt
package lv.mariozo.homeogo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import lv.mariozo.homeogo.ui.ChatMessage
import lv.mariozo.homeogo.ui.ElzaScreen
import lv.mariozo.homeogo.ui.ElzaScreenState
import lv.mariozo.homeogo.ui.InteractionMode
import lv.mariozo.homeogo.ui.viewmodel.ElzaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val vm: ElzaViewModel = viewModel()
            val ui by vm.ui.collectAsState()

            // Adapteris: mūsu minimālais VM stāvoklis -> ElzaScreenState
            val screenState = ElzaScreenState(
                status = ui.status,
                isListening = ui.isListening,
                messages = emptyList<ChatMessage>(),
                speakingMessage = null,
                interactionMode = InteractionMode.CHAT
            )

            ElzaScreen(
                state = screenState,
                onStartListening = { vm.startListening() },
                onStopListening = { vm.stopListening() },
                onToggleMute = { vm.stopSpeaking() },
                onModeSelected = { /* no-op (pagaidām) */ }
            )
        }
    }
}
