# 🧾 CHANGELOG

v1.3.0 – Elza One-Button Dialogue 🎙️ 10.okt.2025

## alvenais:

Viena poga tagad vada visu Elzas sarunu ciklu (zaļa → sarkana → pelēka).

Ieviesta īsā pelēkās pogas darbība – TTS apklusināšana un tūlītēja klausīšanās (“barge-in”).

Garais pelēkais klikšķis vēl nav iedarbināts.

STT darbojas paralēli TTS → lietotājs var pārtraukt ar balsi.

Poga pilnīgi apaļa, centrēta virs sistēmas joslas ar pulsējošu halo.

Pagaidu “…” burbulis parādās domāšanas brīdī un pazūd, ja runa pārtraukta.

Stabilas balss sarunas gan emulatorā, gan telefonā.

## Ietekmētie faili:

ElzaViewModel.kt, ElzaScreen.kt, MainActivity.kt

## Statuss:

✅ Pilns klausīšanās-runas-pārtraukšanas cikls darbojas.
🔜 Nākamais solis – balss režīmu vizuālais indikators un klusuma ikona.

-----------------------------------------------------------------------------

## [1.2.0] – 08.okt.2025

### Added

- Flask backend /health, /elza/reply + Android savienojums
- ElzaViewModel → JSON {"prompt","lang"}; parse {"reply"}
- 350 ms TTS→STT tail guard (novērš pašatbalsi)

### Milestone

Elza gained reason — pirmais pilnais dialogs end-to-end 🎉

**Status:** Stable release  
**Branch:** `main` (backup: `safe/azure-stt-working`)  
**Focus:** Azure STT/TTS integrācija un emulatora mikrofona atbalsts

### 🔧 Galvenās izmaiņas

- Pievienota **Azure Speech SDK** integrācija (`voice/SpeechRecognizerManager.kt`,
  `voice/TtsManager.kt`).
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

-----------------------------------------------------------------------------
`app/build.gradle.kts` → `defaultConfig`:

```kotlin
buildConfigField("String", "AZURE_SPEECH_KEY", "\"<your_key_here>\"")
buildConfigField("String", "AZURE_SPEECH_REGION", "\"northeurope\"")
buildConfigField("String", "STT_LANGUAGE", "\"lv-LV\"")
buildConfigField("boolean", "USE_SYSTEM_STT_ON_EMULATOR", "true")

## v1.2.0 — WIP (04.okt.2025, Rīga) ## v1.1.0 — 04.okt.2025 (Rīga)
**Branch:** `feature/elza-ai-dialogue`  
**Fokuss:** Elzas sarunu režīms (STT → loģika → TTS), saglabājot latviešu valodu (`STT_LANGUAGE="lv-LV"`).

### Plānots šajā versijā
- **ElzaLogicManager.kt**: sarunas loģika (teksta atbildes ģenerēšana), atdalīta no UI.
- **Dialoga UI**: vienkāršs čata skats (lietotāja teiktais / Elzas atbilde).
- **Plūsma**: STT (lv-LV) → loģika → TTS (lv-LV EveritaNeural).
- **Emulatora režīms**: ja vajag, saglabāt sistēmas STT kā fallback (nekādas izmaiņas telefona plūsmā).
- **Dokumentācija**: README sadaļa par valodas iestatījumiem (lv-LV).

### Statuss
- Bāze no v1.1.0 darbojas (telefons + emulators).
- `STT_LANGUAGE` paliek **"lv-LV"**; `AZURE_SPEECH_REGION` paliek **"northeurope"**.

---

