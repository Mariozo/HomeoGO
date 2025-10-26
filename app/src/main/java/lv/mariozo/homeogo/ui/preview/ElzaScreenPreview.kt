// File: app/src/main/java/lv/mariozo/homeogo/ui/preview/ElzaScreenPreview.kt
package lv.mariozo.homeogo.ui.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import lv.mariozo.homeogo.ui.ElzaScreen
import lv.mariozo.homeogo.ui.ElzaScreenState
import lv.mariozo.homeogo.ui.InteractionMode

@Preview(showBackground = true, name = "ElzaScreen – minimal")
@Composable
fun ElzaScreenPreview() {
    MaterialTheme {
        Surface {
            val demo = ElzaScreenState(
                status = "Gatavs (preview)",
                isListening = false,
                messages = emptyList(),
                speakingMessage = null,
                interactionMode = InteractionMode.CHAT
            )
            ElzaScreen(
                state = demo,
                onStartListening = {},
                onStopListening = {},
                onToggleMute = {},
                onModeSelected = {}
            )
        }
    }
}
