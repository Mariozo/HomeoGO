// ApiCompat.kt — compatibility aliases to bridge old vs new class names/locations
// Package MUST be exactly this to satisfy existing imports in ElzaViewModel.
package lv.mariozo.homeogo.voice

// If the real manager class is TTSManager (new name), map old TtsManager -> TTSManager
typealias TtsManager = TTSManager

