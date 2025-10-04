### 🚀 HomeoGO v1.1.0 (04.okt.2025 – Rīga)
Šis ir stabilais HomeoGO laidiena posms ar pilnīgu Azure Speech-to-Text (STT) un Text-to-Speech (TTS) integrāciju.

#### 🔧 Jaunumi
- Pilnībā implementēts Azure STT/TTS, ar drošu `BuildConfig` konfigurāciju (`AZURE_SPEECH_KEY`, `AZURE_SPEECH_REGION`, `STT_LANGUAGE`).
- `ElzaViewModel.kt` atbalsta gan Azure STT (reālām ierīcēm), gan sistēmas STT emulatorā (`USE_SYSTEM_STT_ON_EMULATOR`).
- `ElzaScreen.kt` Preview darbojas Android Studio.
- `MainActivity.kt` automātiski pieprasa mikrofona atļauju.
- Emulatora STT aktivizējas, ja iestatījumos iespējots:
  **Extended Controls → Microphone → Virtual microphone uses host audio input.**
- TTS “Pārbaudīt balsi” darbojas gan emulatorā, gan fiziskajā ierīcē.

#### 🧩 Saderība
| Komponents | Versija |
|-------------|----------|
| compileSdk | 36 |
| targetSdk | 36 |
| minSdk | 24 |
| Kotlin | 1.9.23 |
| Compose Compiler | 1.5.13 |
| Azure Speech SDK | 1.38.0+ |
| Android Gradle Plugin | 8.5.0 |

#### ✅ Testēts uz
- **Samsung Galaxy A52 (Android 14)** — STT un TTS pilnībā funkcionē.
- **Android Emulator (API 34)** — STT darbojas ar “Virtual microphone” iespēju.

#### 🧠 Piezīme
Šis laidiena punkts (`safe/azure-stt-working`) kalpo kā bāze turpmākai HomeoGO attīstībai (v1.2.x un jaunākiem eksperimentiem).

