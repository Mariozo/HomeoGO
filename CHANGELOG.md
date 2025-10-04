# 🧾 CHANGELOG

## v1.1.0 — 04.okt.2025 (Rīga)
**Status:** Stable release  
**Branch:** `main` (backup: `safe/azure-stt-working`)  
**Focus:** Azure STT/TTS integrācija un emulatora mikrofona atbalsts

### 🔧 Galvenās izmaiņas
- Pievienota **Azure Speech SDK** integrācija (`voice/SpeechRecognizerManager.kt`, `voice/TtsManager.kt`).
- **ElzaViewModel.kt** pārbūvēts:
  - Reāls STT/TTS caur `SpeechRecognizerManager` un `TtsManager`.
  - Emulatorā iespējots **System SpeechRecognizer fallback** (`USE_SYSTEM_STT_ON_EMULATOR`).
  - Vienots stāvoklis ar `StateFlow` (`ElzaScreenState`).
- **ElzaScreen.kt** un **MainActivity.kt** saskaņoti ar jaunajiem ViewModel callbackiem.
- **libs.versions.toml** un **build.gradle.kts** salāgoti (Compose 1.7.x, Kotlin 1.9.23, AGP 8.5+).
- **AndroidManifest.xml** papildināts ar `RECORD_AUDIO` un `INTERNET` atļaujām.
- **Preview** darbojas Android Studio bez kritieniem.
- **TTS “Pārbaudīt balsi”** darbojas gan uz emulatora, gan fiziskās ierīces.
- **STT** uz telefona atpazīst balsi precīzi; uz emulatora darbojas, ja ieslēgts:
  > *Extended Controls → Microphone → Virtual microphone uses host audio input.*

### 🧠 Papildu drošības un konfigurācijas lauki
`app/build.gradle.kts` → `defaultConfig`:
```kotlin
buildConfigField("String", "AZURE_SPEECH_KEY", "\"<your_key_here>\"")
buildConfigField("String", "AZURE_SPEECH_REGION", "\"northeurope\"")
buildConfigField("String", "STT_LANGUAGE", "\"lv-LV\"")
buildConfigField("boolean", "USE_SYSTEM_STT_ON_EMULATOR", "true")
