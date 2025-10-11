// File: app/src/main/java/lv/mariozo/homeogo/ui/ElzaScreen.kt
// Project: HomeoGO
// Created: 14.okt.2025 (Rīga)
// ver. 6.1 (FEAT - Add interactionMode to state)
// Purpose: A clean, state-driven UI for the voice assistant with navigation.
// Comments:
//  - Added `interactionMode` to ElzaScreenState to hold the current app mode.

package lv.mariozo.homeogo.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// #1. ---- Data Models & Enums --------------------------------------------------
enum class Sender { USER, ELZA }

enum class InteractionMode {
    SETTINGS, // To open the settings screen
    CHAT,     // Written questions, written answers
    VOICE     // Spoken questions, spoken answers
}

data class ChatMessage(
    val id: Long,
    val from: Sender,
    val text: String,
    val isSpeaking: Boolean = false
)

data class ElzaScreenState(
    val status: String = "Gatavs",
    val isListening: Boolean = false,
    val isMuted: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val speakingMessage: ChatMessage? = null,
    val interactionMode: InteractionMode = InteractionMode.VOICE, // <-- PIEVIENOTS
    internal val currentlySpokenText: String? = null
)

private enum class VoiceMode { Idle, Listening, Speaking }

// #2. ---- Main UI Composable -----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElzaScreen(
    state: ElzaScreenState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onToggleMute: () -> Unit,
    onModeSelected: (InteractionMode) -> Unit, // Callback for drawer item clicks
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet {
                        Spacer(Modifier.height(12.dp))
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Iestatījumi") },
                            label = { Text("Iestatījumi") },
                            selected = state.interactionMode == InteractionMode.SETTINGS,
                            onClick = { onModeSelected(InteractionMode.SETTINGS); scope.launch { drawerState.close() } },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Chat, contentDescription = "Rakstiskais režīms") },
                            label = { Text("Rakstiskais režīms") },
                            selected = state.interactionMode == InteractionMode.CHAT,
                            onClick = { onModeSelected(InteractionMode.CHAT); scope.launch { drawerState.close() } },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Phone, contentDescription = "Telefona režīms") },
                            label = { Text("Telefona režīms") },
                            selected = state.interactionMode == InteractionMode.VOICE,
                            onClick = { onModeSelected(InteractionMode.VOICE); scope.launch { drawerState.close() } },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            },
            gesturesEnabled = drawerState.isOpen
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                ElzaMainContent(
                    state = state,
                    onStartListening = onStartListening,
                    onStopListening = onStopListening,
                    onToggleMute = onToggleMute,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElzaMainContent(
    state: ElzaScreenState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val speaking = state.speakingMessage != null
    val mode = when {
        speaking -> VoiceMode.Speaking
        state.isListening -> VoiceMode.Listening
        else -> VoiceMode.Idle
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Elza") },
                actions = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opcijas")
                    }
                }
            )
        },
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

                val fabIcon = when {
                    mode == VoiceMode.Speaking && state.isMuted -> Icons.Default.MicOff
                    mode == VoiceMode.Speaking -> Icons.Default.Mic
                    else -> Icons.Default.Phone
                }

                val fabIconTint = if (mode == VoiceMode.Speaking && state.isMuted) {
                    Color.Black
                } else {
                    Color.White
                }

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(fabColor)
                        .pointerInput(mode, state.isMuted) {
                            detectTapGestures(
                                onTap = {
                                    when (mode) {
                                        VoiceMode.Speaking -> onStartListening()
                                        VoiceMode.Listening -> onStopListening()
                                        VoiceMode.Idle -> onStartListening()
                                    }
                                },
                                onLongPress = {
                                    if (mode == VoiceMode.Speaking) onToggleMute()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fabIcon,
                        contentDescription = "Voice Assistant Button",
                        modifier = Modifier.size(36.dp),
                        tint = fabIconTint
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        modifier = Modifier.navigationBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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

// #3. ---- UI Sub-components -----------------------------------------------------

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

// #4. ---- Preview ----------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true, name = "ElzaScreen Preview")
@Composable
private fun PreviewElzaScreen() {
    MaterialTheme {
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
            onToggleMute = {},
            onModeSelected = {}
        )
    }
}
