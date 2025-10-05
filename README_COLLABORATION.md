📅 Izveidots: 2025-10-04
📍 Versija: 1.0

# 🤝 HomeoGO – Sadarbības noteikumi (Māris + ChatGPT)

## 🧭 Mērķis
Šis fails apraksta sadarbības kārtību starp **Testētāju (Māri)** un **Kodētāju (ChatGPT)** HomeoGO projekta izstrādē.
Mērķis — nodrošināt, lai katrs kods, fails un solis tiktu veikts saskaņā ar **MK! protokolu** un **SPS principiem**, 
un projekts būtu pilnībā atjaunojams no GitHub repozitorija.

---

## 🧩 Lomas
| Loma | Atbildība |
|------|------------|
| **Māris (Testētājs)** | Dod uzdevumus, pārbauda, testē un apstiprina risinājumus. |
| **ChatGPT (Kodētājs)** | Izstrādā kodu, dokumentāciju un labo kļūdas saskaņā ar MK!/SPS. |

---

## ⚙️ Protokoli

### 🕹️ MK! (Melnās Kastes princips)
> “Māris nedz programmē, nedz labo. ChatGPT labo un izdrukā, Māris testē.”

- Ja labojumu ir **vairāk par vienu** → ChatGPT izdrukā **pilnu failu** ar galviņu.  
- Ja labojumu ir **viens vai neliels** → tiek lietots **MK! ielāps** (`# /X.Y` ... `# X.Y//`).  
- Katram failam — **vienāda galviņas struktūra** un **datuma/z laika zīmogs (Europe/Riga)**.  
- Visi komentāri, galviņas un docstringi — **angliski**, bet skaidrojumi — **latviski**.

### 📖 SPS (Soli pa solim skaidrojumi)
> “Katram skaidrojumam jābūt saprotamam, secīgam un praktiskam.”

- Vienmēr sniedz strukturētus soļus (`1️⃣ 2️⃣ 3️⃣`) un īsu kopsavilkumu.  
- Draudzīgs, precīzs tonis — bez liekvārdiem.  
- Katrs risinājums jāpārbauda uz **Android Studio (Kotlin/Jetpack Compose)** saderību.  

---

## 🧱 Koda standarti

| Nosaukums | Vērtība |
|------------|----------|
| **Valoda** | Kotlin |
| **Redaktors** | Android Studio |
| **Iekļaušana** | 4 atstarpes |
| **Rindu beigas** | LF |
| **Maks. rindu garums** | 120 |
| **Kodējums** | UTF-8 |
| **Stils** | Vienots ar “MK! galviņu” visos failos |

---

## 📄 Failu galviņa (standarta forma)
```kotlin
// ============================================================
// File: app/src/main/java/lv/mariozo/homeogo/MainActivity.kt
// Module: HomeoGO
// Purpose: Main Compose activity
// Created: 04.oct.2025 19:55 (Europe/Riga)
// Version: 1.0
// Author: Māris + ChatGPT
// ============================================================
🔁 Darba plūsma (Workflow)

1️⃣ Māris dod uzdevumu → ar īsu un skaidru formulējumu.
2️⃣ ChatGPT sagatavo risinājumu → kodu, failu vai SPS skaidrojumu.
3️⃣ Māris testē → sniedz “OK” vai kļūdu izdrukas.
4️⃣ ChatGPT labo → līdz “OK”.
5️⃣ Risinājums tiek commit’ots uz GitHub.

🌐 Failu atrašanās un sinhronizācija
Vieta	Paskaidrojums
E:\AndroidStudioProjects\HomeoGO	Lokālā projekta versija
https://github.com/Mariozo/HomeoGO	Oficiālais repozitorijs
cooperation.config.yaml	Tehniskie sadarbības noteikumi
README_COLLABORATION.md	Cilvēkiem saprotamā versija
🤖 Integrētie aģenti
Aģents	Funkcija
ChatGPT-5	Galvenais kodētājs (šī saruna)
Gemini (Android Studio)	Lokālais palīgs – testē, skaidro, izpilda
Elza (TTS aģents)	Balss starpnieks HomeoGO / Burtnieks projektos
🧭 Piezīmes

Sadarbības noteikumi vienmēr tiek glabāti un versēti GitHub repo.

ChatGPT nekad nepārraksta visu failu, ja pietiek ar ielāpu (MK!).

Ja kādā brīdī nepieciešams atsākt no nulles, tiek lietota komanda “No šī brīža: X”.

Ja konteksts kļūst pārāk smags → tiek piedāvāts “īsceļš”.

Autori:
🧑‍💻 Māris — Testētājs / Arhitekts
🤖 ChatGPT-5 — Kodētājs / Dokumentētājs

