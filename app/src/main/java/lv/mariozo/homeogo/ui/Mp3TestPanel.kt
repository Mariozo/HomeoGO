
// File: java/lv/mariozo/homeogo/ui/Mp3TestPanel.kt
// Module: HomeoGO
// Purpose: MP3 test UI (play/stop 3 res/raw clips) for quick audio sanity checks.
// Created: 17.sep.2025 22:55
// ver. 1.0

// #1. ---- Package & Imports ---------------------------------------------------
package lv.mariozo.homeogo.ui

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import lv.mariozo.homeogo.R

// #2. ---- Public API & State --------------------------------------------------
/**
 * Mp3TestPanel
 * - Minimal panel to quickly test raw audio playback (not needed for STT/TTS).
 * - Useful only as a hardware/speaker sanity check.
 */
@Composable
fun Mp3TestPanel() {
    val ctx = LocalContext.current
    val controller = remember { Mp3Controller(ctx) }
    var nowPlaying by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("MP3 Test Panel (sanity check)", style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                controller.play(R.raw.mic_test_short)
                nowPlaying = "mic_test_short"
            }) { Text("Play #1") }

            Button(onClick = {
                controller.play(R.raw.mic_test_mid)
                nowPlaying = "mic_test_mid"
            }) { Text("Play #2") }

            Button(onClick = {
                controller.play(R.raw.mic_test_long)
                nowPlaying = "mic_test_long"
            }) { Text("Play #3") }
        }

        OutlinedButton(onClick = {
            controller.stop()
            nowPlaying = null
        }) { Text("Stop") }

        Text("Now playing: ${nowPlaying ?: "—"}")
    }

    DisposableEffect(Unit) {
        onDispose { controller.release() }
    }
}

// #3. ---- Internals -----------------------------------------------------------
private class Mp3Controller(private val context: Context) {
    private var player: MediaPlayer? = null

    fun play(rawResId: Int) {
        stop()
        player = MediaPlayer.create(context, rawResId)
        player?.setOnCompletionListener { stop() }
        player?.start()
    }

    fun stop() {
        player?.stop()
        player?.reset()
        player?.release()
        player = null
    }

    fun release() = stop()
}
