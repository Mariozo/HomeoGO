// File: app/src/main/java/lv/mariozo/homeogo/ui/ElzaScreen.kt
// Project: HomeoGO
// Created: 18.okt.2025 - 15:10
// ver. 2.6 (SPS-46: Fix Preview rendering and add Listening state)
// Purpose: Elza voice interaction screen (Compose UI)
// Comments:
//  - Added a new preview for the 'Listening' state.
//  - Ensured the `stringResource` is correctly resolved in all previews.

package lv.mariozo.homeogo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import lv.mariozo.homeogo.R
import lv.mariozo.homeogo.ui.theme.HomeoGOTheme
import kotlin.math.cos
import kotlin.math.sin

// # 1. ---- Data classes & Enums ----
enum class InteractionMode { VOICE, TEXT, HYBRID, SETTINGS }
enum class Sender { USER, ELZA }
data class ChatMessage(
    val id: Long,
    val from: Sender,
    val text: String,
    val isSpeaking: Boolean = false,
)

data class ElzaScreenState(
    val status: String = "Gatavs",
    val isListening: Boolean = false,
    val isMuted: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val speakingMessage: ChatMessage? = null,
    val interactionMode: InteractionMode = InteractionMode.VOICE,
    val error: String? = null,
    val permissionDenied: Boolean = false,
    val audioLevel: Float = 0f,
    val time: Long = 0L,
    val isProcessing: Boolean = false,
    val isThinking: Boolean = false,
    val isReplying: Boolean = false,
)


// # 2. ---- Main Screen Composable ----
@Composable
fun ElzaScreen(
    state: ElzaScreenState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onToggleMute: () -> Unit,
    onModeSelected: (InteractionMode) -> Unit,
) {
    var textInput by rememberSaveable { mutableStateOf("") }
    val showTextField by remember { derivedStateOf { state.interactionMode != InteractionMode.VOICE } }
    val showSpeakButton by remember { derivedStateOf { state.interactionMode != InteractionMode.TEXT } }

    val isMicButtonPressed = remember { MutableInteractionSource() }
    val isPressed by isMicButtonPressed.collectIsPressedAsState()
    LaunchedEffect(isPressed) {
        if (isPressed) onStartListening() else onStopListening()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ElzaOrbWithStatus(state, Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ModeSelector(currentMode = state.interactionMode, onModeSelected = onModeSelected)
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (showTextField) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.elza_input_placeholder)) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        ),
                        trailingIcon = {
                            IconButton(onClick = { /* TODO: Send text */ }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.elza_send_button_description)
                                )
                            }
                        }
                    )
                }
                if (showSpeakButton) {
                    if (showTextField) Spacer(Modifier.width(16.dp))
                    SpeakButton(
                        isListening = state.isListening,
                        interactionSource = isMicButtonPressed,
                        modifier = if (!showTextField) Modifier.size(90.dp) else Modifier.size(56.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            MuteButton(isMuted = state.isMuted, onToggle = onToggleMute)
        }
    }
}

// # 3. ---- Status & Orb Composables ----
@Composable
fun ElzaOrbWithStatus(state: ElzaScreenState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ElzaOrb(state, modifier = Modifier.size(200.dp))
        Spacer(Modifier.height(32.dp))
        StatusText(state)
    }
}

@Composable
fun StatusText(state: ElzaScreenState) {
    val statusText = when {
        state.isListening -> stringResource(R.string.elza_listening_status)
        state.isProcessing -> stringResource(R.string.elza_processing_status)
        state.isThinking -> stringResource(R.string.elza_thinking_status)
        state.isReplying -> stringResource(R.string.elza_replying_status)
        state.isMuted -> stringResource(R.string.elza_muted_status)
        state.error != null -> "${stringResource(R.string.elza_error_status)}: ${state.error}"
        state.permissionDenied -> stringResource(R.string.elza_permission_denied_status)
        else -> state.status
    }
    AnimatedContent(
        targetState = statusText,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(
                animationSpec = tween(
                    300
                )
            )
        },
        label = "StatusTextAnimation"
    ) { text ->
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@Composable
fun ElzaOrb(state: ElzaScreenState, modifier: Modifier = Modifier) {
    val isBusy = state.isListening || state.isThinking || state.isProcessing || state.isReplying
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )
    val busyGlowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val radius = constraints.maxWidth / 2f
        val animatedProgress by animateFloatAsState(
            targetValue = state.audioLevel,
            animationSpec = tween(100),
            label = "AudioLevel"
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val time = state.time.toDouble()
            val angleOffset = time * 0.1

            if (isBusy) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(busyGlowColor, Color.Transparent),
                        radius = radius * 1.1f
                    ),
                    radius = radius * 1.1f
                )
            }
            for (i in colors.indices) {
                val angle = (i * 120.0 + angleOffset) * (Math.PI / 180.0)
                val waveOffset = sin(time * 0.5 + i) * radius * 0.1f
                val currentRadius = radius * 0.7f + waveOffset
                val x = (center.x + cos(angle) * currentRadius).toFloat()
                val y = (center.y + sin(angle) * currentRadius).toFloat()

                drawCircle(
                    color = colors[i].copy(alpha = (0.5f + (sin(time + i) * 0.2f)).toFloat()),
                    radius = radius * 0.1f + animatedProgress * radius * 0.1f,
                    center = Offset(x, y)
                )
            }
        }
        if (state.isThinking || state.isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(0.6f),
                color = MaterialTheme.colorScheme.onSurface,
                strokeWidth = 2.dp
            )
        }
    }
}

// # 4. ---- Buttons & Selectors ----
@Composable
fun SpeakButton(
    isListening: Boolean,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isListening) 1.2f else 1.0f,
        animationSpec = tween(200),
        label = "SpeakButtonScale"
    )
    val color =
        if (isListening) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .clickable(interactionSource = interactionSource, indication = null, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = stringResource(R.string.elza_speak_button_description),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
fun MuteButton(isMuted: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.VolumeOff,
            contentDescription = stringResource(R.string.elza_mute_button_description),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isMuted) 1.0f else 0.5f)
        )
    }
}

@Composable
fun ModeSelector(currentMode: InteractionMode, onModeSelected: (InteractionMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(24.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        InteractionMode.entries.forEach { mode ->
            val isSelected = currentMode == mode
            val backgroundAlpha by animateFloatAsState(
                if (isSelected) 1f else 0f,
                label = "ModeSelectorAlpha"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = backgroundAlpha))
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (mode) {
                        InteractionMode.VOICE -> stringResource(R.string.elza_mode_voice)
                        InteractionMode.TEXT -> stringResource(R.string.elza_mode_text)
                        InteractionMode.HYBRID -> stringResource(R.string.elza_mode_hybrid)
                        InteractionMode.SETTINGS -> stringResource(R.string.elza_mode_settings)
                    },
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// # 5. ---- Previews ----
@Preview(showBackground = true, name = "Idle State")
@Composable
private fun ElzaScreenPreview_Idle() {
    HomeoGOTheme {
        ElzaScreen(
            state = ElzaScreenState(),
            onStartListening = {},
            onStopListening = {},
            onToggleMute = {},
            onModeSelected = {}
        )
    }
}

@Preview(showBackground = true, name = "Listening State")
@Composable
private fun ElzaScreenPreview_Listening() {
    HomeoGOTheme {
        ElzaScreen(
            state = ElzaScreenState(isListening = true, audioLevel = 0.8f),
            onStartListening = {},
            onStopListening = {},
            onToggleMute = {},
            onModeSelected = {}
        )
    }
}
