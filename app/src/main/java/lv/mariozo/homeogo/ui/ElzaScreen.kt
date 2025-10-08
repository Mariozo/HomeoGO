// File: app/src/main/java/lv/mariozo/homeogo/ui/ElzaScreen.kt
// Project: HomeoGO
// Created: 04.okt.2025 4:10 (Rīga)
// ver. 1.9
// Purpose: Elza screen UI. Uses Scaffold to respect window insets (safe areas).
// Comments:
//  - Added missing import for HomeoGOTheme to fix Previews.

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import lv.mariozo.homeogo.ui.theme.HomeoGOTheme

// # 2. ---- Public UI -----------------------------------------------------------
@Composable
fun ElzaScreen(
    state: ElzaScreenState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onClearChat: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Text(
                text = "Elza",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        },
        bottomBar = {
            ControlsRow(
                isListening = state.isListening,
                onStartListening = onStartListening,
                onStopListening = onStopListening,
                onClearChat = onClearChat,
                modifier = Modifier
                    .padding(16.dp) // Add padding around the controls
                    .navigationBarsPadding() // Add padding for the navigation bar
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Apply padding from Scaffold
                .padding(horizontal = 16.dp) // Keep horizontal padding
        ) {
            // # 2.2 ---- Chat list ------------------------------------------------
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
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
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
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focus = LocalFocusManager.current

    Column(modifier.fillMaxWidth()) {
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

            Button(
                onClick = {
                    focus.clearFocus()
                    onClearChat()
                }
            ) { Text("Notīrīt") }
        }
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
    HomeoGOTheme {
        ElzaScreen(
            state = mock,
            onStartListening = {},
            onStopListening = {},
            onClearChat = {}
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
    HomeoGOTheme {
        ElzaScreen(
            state = mock,
            onStartListening = {},
            onStopListening = {},
            onClearChat = {}
        )
    }
}
