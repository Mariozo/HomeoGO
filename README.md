<!-- 
File: README.md
Module: HomeoGO
Purpose: Project overview & quickstart for the Android (Jetpack Compose) app
Created: 17.09.2025 18:23
ver. 1.1
-->

# HomeoGO

HomeoGO is an Android app built with **Jetpack Compose + Material 3**.  
It includes a minimal **STT/TTS skeleton (“Elza”)** that can:
- listen via Android `SpeechRecognizer` (`lv-LV` or `en-US`),  
- speak replies via Android **TextToSpeech** (prefers Google TTS + Latvian voice).

---

## Status
MVP under development.  
Latest milestone: **STT works on Samsung A52s** (status “Listening…” and error handling).  
UI shows smoke test message: *“HomeoGO is running 🚀”*.

---

## Requirements
- **Android Studio** Ladybug 2024.2.2 Patch 1 (or newer)  
- **JDK 21** (JetBrains Runtime bundled with AS is fine)  
- **Android SDK**: target/compile 35, minSdk 24  
- **Physical device** recommended for STT (emulator mic is unreliable)  
- **Network** required for some STT languages (e.g. Latvian)  

---

## Project structure
```
app/
  src/main/
    AndroidManifest.xml
    res/
      values/ -> themes.xml (light)
      values-night/ -> themes.xml (dark)
    java/lv/mariozo/homeogo/
      MainActivity.kt -> entry point (SmokeTestScreen)
      ui/theme/ -> HomeoGOTheme (Compose M3 theme)
      voice/TTSManager.kt -> TextToSpeech wrapper (prefers lv-LV)
      # STT: SpeechRecognizerManager + ElzaViewModel (in progress)
```

---

## Quickstart
1. Clone the repo:  
   ```bash
   git clone https://github.com/Mariozo/HomeoGO.git
   cd HomeoGO
   ```
2. Open in **Android Studio**.  
3. Run on a **physical device**.  
4. You should see *“HomeoGO is running 🚀”*.  
5. Switch to the **Elza screen** (when integrated) to test STT/TTS.  

---

## Theming
- XML base theme: `Theme.HomeoGO` in `res/values/themes.xml` (+ night).  
- Compose: `HomeoGOTheme` wraps root Composable.  
- Pure Compose activity: no per-activity XML theme.  

---

## Speech-to-Text (STT)
- Language set via `RecognizerIntent.EXTRA_LANGUAGE`: `"lv-LV"` or `"en-US"`.  
- Status messages: `SRM_PARTIAL_RAW`, `SRM_FINAL_RAW`, errors.  

**Common issue: ERROR_NO_MATCH (7)**  
- Emulator mic often fails → use a physical device.  
- Ensure Google app / speech service is updated.  
- Try longer, clear phrases: “Labdien! Kā Tev klājas šodien?”  

---

## Text-to-Speech (TTS)
- `voice/TTSManager.kt`:  
  - Prefers **Google TTS** (`com.google.android.tts`)  
  - Picks Latvian voice if available, else `Locale("lv","LV")`  
- Device settings: Settings → General management → Text-to-Speech  

---

## Version catalog
Dependencies managed via `gradle/libs.versions.toml`.  
Example: `libs.androidx.compose.bom`, `libs.material`.  

---

## Git basics
First push:
```bash
git init -b main
git add .
git commit -m "chore: initial commit"
git remote add origin https://github.com/Mariozo/HomeoGO.git
git push -u origin main
```

---

## Troubleshooting
- **Device not detected**:  
  - On phone: enable USB debugging, revoke authorizations, reconnect.  
  - On PC: `adb kill-server && adb start-server && adb devices`.  
  - Install Samsung/Google USB drivers.  

- **Black screen**:  
  - Ensure `MainActivity` calls `HomeoGOTheme { ... }` and shows a Composable.  

---

## License
TBD.

---

![screenshot placeholder](docs/screenshot.png)
