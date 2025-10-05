// File: app/src/main/java/lv/mariozo/homeogo/ui/ElzaScreen.kt
// Project: HomeoGO
// Created: 04.okt.2025 4:10 (Rīga)
// ver. 1.4
// Purpose: Elza screen UI. Adds chat bubble list (messages) above controls,
//          keeps existing buttons (Klausos/Stop/Pārbaudīt balsi). Stable M3 API.
// Comments:
//  - UI texts LV; code/comments EN. No direct Azure imports; pure callbacks from VM.
//  - Provides two Previews: light & dark, with mock state.

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package lv.mariozo.homeogo.ui

// # 1. ---- Imports -------------------------------------------------------------
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

// # 2. ---- Public UI -----------------------------------------------------------
@Composable
fun ElzaScreen(
    state: ElzaScreenState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSpeakTest: (String) -> Unit,
) {
    // # 2.1 ---- Layout root ----------------------------------------------------
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // Title
            Text(
                text = "Elza",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            // # 2.2 ---- Chat list ------------------------------------------------
            Spacer(Modifier.height(8.dp))
            ChatList(
                messages = state.messages,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            // # 2.3 ---- Status line ---------------------------------------------
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Stāvoklis: ${state.status}",
                style = MaterialTheme.typography.bodyMedium
            )

            // # 2.4 ---- Controls (existing) -------------------------------------
            Spacer(Modifier.height(12.dp))
            ControlsRow(
                isListening = state.isListening,
                onStartListening = onStartListening,
                onStopListening = onStopListening,
                onSpeakTest = onSpeakTest
            )
        }
    }
}

// # 3. ---- Chat bubbles --------------------------------------------------------
@Composable
private fun ChatList(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        reverseLayout = false
    ) {
        items(messages, key = { it.id }) { msg ->
            ChatBubble(
                text = msg.text,
                isElza = msg.from == Sender.ELZA,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ChatBubble(
    text: String,
    isElza: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg =
        if (isElza) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
    val align = if (isElza) Alignment.CenterStart else Alignment.CenterEnd
    val shape = RoundedCornerShape(
        topStart = 16.dp, topEnd = 16.dp,
        bottomStart = if (isElza) 4.dp else 16.dp,
        bottomEnd = if (isElza) 16.dp else 4.dp
    )

    Row(modifier = modifier, horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(if (isElza) Alignment.Start else Alignment.End)
                .clip(shape)
                .background(bg)
                .padding(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// # 4. ---- Controls (existing actions) ----------------------------------------
@Composable
private fun ControlsRow(
    isListening: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSpeakTest: (String) -> Unit,
) {
    var testText by remember { mutableStateOf("Sveika, Elza!") }
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onStartListening,
                enabled = !isListening
            ) { Text("Klausos") }

            Button(
                onClick = onStopListening,
                enabled = isListening
            ) { Text("Stop") }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = testText,
            onValueChange = { testText = it },
            label = { Text("Teksts TTS pārbaudei") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                focus.clearFocus()
                onSpeakTest(testText)
            }
        ) { Text("Pārbaudīt balsi") }
    }
}

// # 5. ---- Previews ------------------------------------------------------------
@Preview(showBackground = true, showSystemUi = true, name = "Light")
@Composable
fun ElzaPreview_Light() {
    val mock = ElzaScreenState(
        status = "Gatava",
        recognizedText = "",
        isListening = false,
        messages = listOf(
            ChatMessage(1, Sender.USER, "Sveika, Elza!"),
            ChatMessage(2, Sender.ELZA, "Sveika! Kā varu palīdzēt?")
        )
    )
    MaterialTheme {
        ElzaScreen(
            state = mock,
            onStartListening = {},
            onStopListening = {},
            onSpeakTest = {}
        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark"
)
@Composable
fun ElzaPreview_Dark() {
    val mock = ElzaScreenState(
        status = "Klausos…",
        recognizedText = "…",
        isListening = true,
        messages = listOf(
            ChatMessage(1, Sender.USER, "Kas jauns?"),
            ChatMessage(2, Sender.ELZA, "Varu pastāstīt par laikapstākļiem vai tavu sarakstu.")
        )
    )
    MaterialTheme {
        ElzaScreen(
            state = mock,
            onStartListening = {},
            onStopListening = {},
            onSpeakTest = {}
        )
    }
}
