// File: app/src/main/java/lv/mariozo/homeogo/ui/ElzaScreen.kt
// Project: HomeoGO
// Created: 12.okt.2025 (Rīga)
// ver. 5.1 (FIX - Build error and code cleanup)
// Purpose: A clean, state-driven UI for the voice assistant.
// Comments:
//  - Removed invalid import that caused a build error.
//  - Hoisted 'mode' and 'speaking' variables to the top of the Composable for clarity.

package lv.mariozo.homeogo.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private enum class VoiceMode { Idle, Listening, Speaking }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ElzaScreen(
    state: ElzaScreenState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onToggleMute: () -> Unit,
) {
    val speaking = state.speakingMessage != null
    val mode = when {
        speaking -> VoiceMode.Speaking
        state.isListening -> VoiceMode.Listening
        else -> VoiceMode.Idle
    }

    Scaffold(
        floatingActionButton = {
            Box(contentAlignment = Alignment.Center) {
                PulsingHalo(
                    visible = speaking || state.status.contains("Domā"),
                    color = if (speaking) Color(0xFF616161) else Color(0xFF00C853)
                )

                val fabColor = when (mode) {
                    VoiceMode.Idle -> Color(0xFF00C853)
                    VoiceMode.Listening -> Color(0xFFD50000)
                    VoiceMode.Speaking -> if (state.isMuted) Color(0xFFFFA000) else Color(0xFF9E9E9E)
                }

                FloatingActionButton(
                    onClick = {
                        when (mode) {
                            VoiceMode.Speaking -> onStartListening()   // interrupt → VM apklusinās TTS un sāks STT
                            VoiceMode.Listening -> onStopListening()   // stop STT
                            VoiceMode.Idle -> onStartListening()       // start STT
                        }
                    },
                    shape = CircleShape,
                    containerColor = fabColor,
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(76.dp)
                        .combinedClickable(
                            onClick = { /* handled by primary FAB onClick */ },
                            onLongClick = { if (mode == VoiceMode.Speaking) onToggleMute() }
                        )
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Voice Assistant Button")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        modifier = Modifier.navigationBarsPadding() // Apply edge-to-edge padding to Scaffold
    ) { innerPadding -> // Scaffold provides padding for the content area

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Apply the padding to prevent content from going under the FAB
        ) {
            val listState = rememberLazyListState()
            val messageCount = state.messages.size + if (state.speakingMessage != null) 1 else 0
            LaunchedEffect(messageCount) {
                if (messageCount > 0) listState.animateScrollToItem(messageCount)
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                items(state.messages) { MessageBubble(it) }
                state.speakingMessage?.let { item { MessageBubble(it) } }
            }

            StatusText(state.status)
        }
    }
}

@Composable
private fun StatusText(status: String) {
    Text(
        text = status,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isElza = msg.from == Sender.ELZA
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isElza) Arrangement.Start else Arrangement.End
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isElza) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(MaterialTheme.shapes.large)
        ) {
            if (msg.isSpeaking) {
                PulsingSpeakingIndicator(modifier = Modifier.padding(14.dp))
            } else {
                Text(
                    text = msg.text,
                    color = if (isElza) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun PulsingSpeakingIndicator(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "ellipsis")
    val alphas = List(3) { i ->
        infinite.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = i * 100),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha$i"
        ).value
    }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        alphas.forEach { Dot(it) }
    }
}

@Composable
private fun Dot(alpha: Float) = Box(
    Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
)

@Composable
private fun PulsingHalo(visible: Boolean, color: Color) {
    if (!visible) return
    val infinite = rememberInfiniteTransition(label = "halo")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )
    val alpha by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alpha"
    )
    Canvas(
        modifier = Modifier
            .size(88.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
    ) {
        drawCircle(color = color)
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "ElzaScreen Preview")
@Composable
private fun PreviewElzaScreen() {
    ElzaScreen(
        state = ElzaScreenState(
            status = "Runā...",
            isListening = false,
            isMuted = true,
            messages = listOf(ChatMessage(1, Sender.USER, "Pastāsti man par kvantu skaitļošanu.")),
            speakingMessage = ChatMessage(2, Sender.ELZA, "...", isSpeaking = true)
        ),
        onStartListening = {},
        onStopListening = {},
        onToggleMute = {}
    )
}
