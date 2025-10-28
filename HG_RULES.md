# HomeoGO — Koda sadarbības noteikumi v1.1

> **Mērķis**: skaidri, īsi un nepārprotami noteikumi, lai ChatGPT = "Koda rakstītājs", Māris = "
> Testētājs" var strādāt bez liekām interpretācijām.

---

## 1) Pamatprincipi

- **Patiesums > ātrums**: bez minējumiem. Ja trūkst datu, tiek prasīts minimālais nepieciešamais
  konteksts.
- **Minimālas izmaiņas**: kļūdu labojumos (FIX režīms) — tikai mazākais iespējamais ielāps.
- **Reproducējamība**: vienmēr ar skaidriem failu ceļiem, commit hash, soļiem.
- **Atdalām radošumu**: ieteikumi un refaktori **pēc** kļūdas labojuma, atsevišķā sadaļā.

---

## 2) Režīmi (Testētājs nosaka katrā uzdevumā)

- **MODE: FIX** — mērķis: novērst vienu konkrētu kļūdu.
    - **Change Budget**: max 1–2 faili, max ~30 rindas; izvairīties no API maiņām.
    - Izeja: ielāps (patch) vai pilns fails (skat. 6. nodaļu).
- **MODE: BUILD** — mērķis: jauna funkcija/struktūra.
    - Plašāks risinājums, strukturēts kods, tests (ja prasa), ieteikumi pievienoti.

---

## 3) GH-FIRST (obligāts noteikums kļūdu analīzē)

**Vienmēr sākam ar GitHub/Repo stāvokli.**

1. Identificējam repo, branch, commit: `REPO, BRANCH, COMMIT` (skat. 4. nodaļu).
2. Ja kļūda parādījās “pēc izmaiņām” — salīdzinām ar pēdējo zināmo labu stāvokli:
   `git diff <good_commit>..<bad_commit>` vai konkrētiem failiem.
3. Pārbaudām CI/Build logus, README/CHANGELOG, konfigurācijas fails.
4. Tikai tad analizējam lokālos logus/stacktrace un piedāvājam ielāpu.
5. Risinājuma tekstā norādām, pēc kādiem **pierādījumiem** (diff, logs, konfigurācija) spriežam.

**Minimālās komandas (piemērs):**

```bash
git fetch --all --prune
git status
git rev-parse HEAD
git log -1 --oneline
# ja zināms labais commit
git diff <good>..<bad> -- path/to/File.kt
```

---

## 4) “Prompt Header” — minimālā galvene katram uzdevumam

Testētājs ieliek uzdevuma sākumā:

```
[HOMEOGO RULES v1.0]
MODE: FIX | BUILD
REPO: <URL vai lokālais ceļš>
BRANCH: <nosaukums>
COMMIT: <hash vai "current">
FILES: <pilnie ceļi, ar ko strādāt>
ERROR: <īsais kļūdas teksts / stacktrace fragments>
OUTPUT: patch | full-file
BOUNDS: <piem., max-changes=1 file|30 lines; no-refactors>
EXTRA: <cits būtiskais (OS, IDE versijas, atslēgas, u.tml.)>
```

> Ja kāds lauks nav zināms, tas jāatstāj tukšs vai “unknown”.

---

## 5) Pierādījumu kārtība (triāža)

1) **Repo stāvoklis/diff** → 2) **Build/CI/logs** → 3) **Konfigurācija/versijas** → 4) **Minimāls
   ielāps** → 5) **Pārbaudes soļi**.

---

## 6) Izvades formāts (ko dod Koda rakstītājs)

- **FIX (1 mazs labojums)**: iedodu **ielāpu (patch)** ar numurētiem blokiem un precīzu kontekstu (
  pirms/pēc rindām).
- **FIX (vairāki labojumi vai vairāk par 1 failu)**: iedodu **pilnu failu(-us)**.
- **BUILD**: vienmēr **pilni faili** + īsi testēšanas soļi.

**Katram failam virsraksta galviņa (EN komentāri):**

```
// File: <path>
// Purpose: <one-line>
// Change: <short summary>
// Timestamp: <YYYY-MM-DD HH:mm, Europe/Riga>
// Commit/Ref: <if applicable>
```

---

## 7) Radošuma robežas

- FIX režīmā: **neveicu** refaktorus, pārvietošanas, API maiņas, bibliotēku jauninājumus — ja vien
  Testētājs nav devis “ALLOW-REFactor: true”.
- Ideju sadaļa “**Pēc ielāpa**” — ieteikumi 1–3 punkti, bez koda maiņas.

---

## 8) Pārlūkošana un citēšana

- Ja fakti var būt mainījušies (SDK, API, cenas, dokumentācija) — **pārlūkošana obligāta**, ar avotu
  citātiem.
- Iekšējām repo lietām — balstāmies uz difiem, failiem, commit vēsturi.

---

## 9) Trausu/Resursietilpīgu soļu brīdinājums

- Pirms “smagiem” soļiem (ilgstoša refaktorizācija, masveida meklēšana/aizstāšana) — tiek dots
  brīdinājums un piedāvāta vieglāka alternatīva.

---

## 10) Kur glabāt noteikumus (enforceability)

- **Repo sakne**: `RULES.md` (šis dokuments). Tas ir vienīgais “source of truth”.
- **ChatGPT Canvas**: dzīvais melnraksts (šis fails) ātrai atsaucei un rediģēšanai.
- **Sarunu atmiņa**: pēc Testētāja apstiprinājuma noteikumus saglabājam ChatGPT atmiņā.
- Katrā jaunā sesijā Testētājs var lietot īsceļu: `LOAD RULES v1.0` (es apstiprinu, ka tās ir
  ielādētas) un turpmāk atsaukšos uz tiem kā uz obligātiem.

---

## 11) Kā padarīt “GH-FIRST” praktiski neapejamu

- `Prompt Header` ir **obligāts** FIX uzdevumos; ja trūkst `REPO/COMMIT`, es vispirms lūdzu to
  norādīt.
- Risinājuma sākumā vienmēr ir **“GH-FIRST CHECK”** punkts ar 1–2 teikumiem: kāds ir repo stāvoklis,
  uz kādas diffa informācijas balstos.

---

## 12) Ātrā starta šabloni

**FIX — mazs ielāps**

```
[HOMEOGO RULES v1.0]
MODE: FIX
REPO: https://github.com/<user>/<repo>
BRANCH: main
COMMIT: current
FILES: app/src/main/java/.../ElzaViewModel.kt
ERROR: Unresolved reference 'settings' line 120
OUTPUT: patch
BOUNDS: max-changes=1 file|20 lines; no-refactors
EXTRA: Android Studio 2024.2.2; Kotlin 1.9.23
```

**BUILD — jauna funkcija**

```
[HOMEOGO RULES v1.0]
MODE: BUILD
REPO: https://github.com/<user>/<repo>
BRANCH: feature/elza-tts
COMMIT: current
FILES: <kas jāizveido>
OUTPUT: full-file
BOUNDS: keep simple; mvvm; no external libs unless ok
EXTRA: target SDK 34; Compose Material3
```

---

## 13) Ieteikts commit stils

- `fix(elza): <īss kļūdas cēlonis> — <risinājuma atslēgvārdi>`
- `feat(homeogo): <jauna iespēja>`
- `docs: add RULES v1.0`

---

## 14) Ievietošanas soļi repo (piemērs)

```bash
# saknes mapē
copy "HomeoGO — Koda sadarbības noteikumi v1.0 (melnraksts).md" RULES.md
git add RULES.md
git commit -m "docs: add HomeoGO collaboration rules v1.0"
git push origin main
```

---

## 15) Android Studio Git panelis — obligātais minimums FIX triāžai

- **Show History (File/Directory)**: ar peles labo → **Git → Show History**. Izvēlies *good* vs
  *bad* commit, **Compare with Selected**.
- **Annotate / Blame**: ar peles labo → **Annotate**. Atrodi ievadošo commit, klikšķis uz malas →
  atver atbilstošo diff.
- **Compare with Branch/Tag/Commit**: **VCS → Git → Compare with Branch…** (piem., `origin/main` vai
  zināmais labs hash).
- **Local History (IDE līmenis)**: ar peles labo → **Local History → Show History**. Mazām kļūdām —
  **Revert** konkrētās rindas.
- **Shelf (Stash IDE stilā)**: **Commit** rīklogā **Shelf** cilne → **Shelve Changes** pirms
  riskiem; pēc tam **Unshelve**.
- **Resolve Conflicts**: Git rīklogā **Merge** (3‑virzienu). Nekad akli **Accept Yours/Theirs** —
  pārbaudi diff.
- **Partial Commit (hunk staging)**: **Commit** panelī atķeksē tikai mainītās rindas; saglabā ielāpu
  šauru.
- **Revert / Cherry‑pick** (no Git Log): peles labais uz commit → **Revert** (droši atgriež), *
  *Cherry‑pick** (ar *No commit* ja vajag tikai ielāpu).

**Triāžas secība GH‑FIRST ietvarā (Android Studio versija):**

1) Show History → salīdzini *good vs bad*. 2) Annotate → atrod commit, kas ieviesa rindu. 3) Compare
   with Branch/Commit → precīzs diff. 4) Ja jātestē riski → **Shelf**. 5) Ielāps ar **Partial Commit
   **.

---

## 16) Prompt Header — mikroformāts un automatizācija

**Mikro header (3 lauki, pārējais pēc noklusējuma):**

```
[HGO v1] FIX file=app/src/.../ElzaViewModel.kt err="Unresolved reference 'settings' line 120" out=patch
```

**Paplašinātais variants (pusrindiņa ar semikoliem):**

```
[HGO v1] FIX repo=https://github.com/<user>/<repo>; branch=main; file=app/src/.../ElzaViewModel.kt; err="..."; out=patch; bounds=1f|30l
```

**Noklusējumi, ja nav norādīti:** `repo=last-used`, `branch=current`, `commit=current`,
`bounds=max-changes=1 file|30 lines`, `output=patch (FIX)`.

**Automatizācija (ChatGPT pusē)**

- Pieņemu mikro header tieši tekstā; es pats to “iztulkošu” uz pilno Header.
- Ja pietrūkst kritiska lauka (piem., `file` vai `err`), uzprasīšu **vienu reizi** un turpināšu.

**Automatizācija (Tava pārlūka pusē)**

- **Tampermonkey** skripts “Header Helper” pievieno peldošu pogu: aizpildi formu vienu reizi, tiek
  saglabāts `last-used`, un ar vienu klikšķi ielīmē **Prompt Header**.

---

### Statuss

Šis dokuments ir **v1.1** un ir uzskatāms par aktuālo “source of truth”.

---

## Pielikums A — Tampermonkey “Header Helper” (mikro header ielīmēšanai)

```javascript
// ==UserScript==
// @name         HomeoGO Header Helper
// @namespace    hgo-helper
// @match        https://chatgpt.com/*
// @match        https://chat.openai.com/*
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_addStyle
// ==/UserScript==
(function () {
  const css = `
    #hgo-helper {position:fixed; right:12px; bottom:12px; z-index:9999; padding:8px 12px; border-radius:10px; box-shadow:0 2px 10px rgba(0,0,0,.15); background:#fff; cursor:pointer; font:14px/1.2 system-ui}
    #hgo-form {position:fixed; right:12px; bottom:52px; z-index:10000; background:#fff; padding:12px; border-radius:12px; width:320px; box-shadow:0 6px 24px rgba(0,0,0,.2); display:none}
    #hgo-form input, #hgo-form textarea, #hgo-form select {width:100%; margin:6px 0; padding:6px 8px; border:1px solid #ddd; border-radius:8px; font:13px system-ui}
    #hgo-actions {display:flex; gap:8px; margin-top:8px}
    #hgo-actions button {flex:1; padding:8px; border-radius:8px; border:1px solid #ddd; background:#f5f5f5; cursor:pointer}
  `;
  GM_addStyle(css);

  const btn = document.createElement('button');
  btn.id = 'hgo-helper';
  btn.textContent = 'Header Helper';
  document.body.appendChild(btn);

  const form = document.createElement('div');
  form.id = 'hgo-form';
  form.innerHTML = `
    <div style="font-weight:600;margin-bottom:6px">HomeoGO — mikro header</div>
    <label>Mode</label>
    <select id="hgo-mode"><option>FIX</option><option>BUILD</option></select>
    <label>Repo (optional)</label>
    <input id="hgo-repo" placeholder="https://github.com/<user>/<repo>">
    <label>Branch (optional)</label>
    <input id="hgo-branch" placeholder="main">
    <label>File</label>
    <input id="hgo-file" placeholder="app/src/.../ElzaViewModel.kt" required>
    <label>Error</label>
    <textarea id="hgo-err" rows="2" placeholder="Short error / stacktrace" required></textarea>
    <label>Output</label>
    <select id="hgo-out"><option>patch</option><option>full-file</option></select>
    <label>Bounds (optional)</label>
    <input id="hgo-bounds" placeholder="1f|30l">
    <div id="hgo-actions">
      <button id="hgo-insert">Insert</button>
      <button id="hgo-close">Close</button>
    </div>
  `;
  document.body.appendChild(form);

  const $ = id => form.querySelector(id);
  ['hgo-repo','hgo-branch','hgo-file','hgo-err','hgo-bounds'].forEach(k=>{ const v = GM_getValue(k,''); if(v) $( '#'+k ).value = v; });

  btn.onclick = () => { form.style.display = form.style.display==='none' ? 'block' : 'none'; };
  $('#hgo-close').onclick = () => { form.style.display = 'none'; };

  $('#hgo-insert').onclick = async () => {
    const mode = $('#hgo-mode').value.trim() || 'FIX';
    const repo = $('#hgo-repo').value.trim();
    const branch = $('#hgo-branch').value.trim();
    const file = $('#hgo-file').value.trim();
    const err  = $('#hgo-err').value.trim();
    const out  = $('#hgo-out').value.trim();
    const bounds = $('#hgo-bounds').value.trim();

    if (!file || !err) { alert('File un Error ir obligāti.'); return; }

    const parts = [`[HGO v1] ${mode} file=${file} err="${err}" out=${out}`];
    if (repo) parts.splice(1,0,`repo=${repo}`);
    if (branch) parts.splice(2,0,`branch=${branch}`);
    if (bounds) parts.push(`bounds=${bounds}`);

    const text = parts.join(' ');
    [['hgo-repo',repo],['hgo-branch',branch],['hgo-file',file],['hgo-err',err],['hgo-bounds',bounds]].forEach(([k,v])=>GM_setValue(k,v));

    const editor = document.querySelector('[contenteditable="true"]');
    if (editor) {
      editor.focus();
      document.execCommand('insertText', false, text + '

');
    } else {
      await navigator.clipboard.writeText(text + '

');
      alert('Mikro header nokopēts uz starpliktuvi — ielīmē čatā (Ctrl+V).');
    }
    form.style.display = 'none';
  };
})();
```

---

## Pielikums B — `RULES.md` ievietošana repo (Android Studio vai CLI)

**Android Studio (VCS → Git):**

1. Failu pārdēvē Canvasā par `RULES.md` un ieliec repo saknē.
2. Atver **Commit** paneli → iezīmē `RULES.md` → pievieno ziņu:
   `docs: add HomeoGO collaboration rules v1.1` → **Commit**.
3. **Push** uz `origin/<branch>` (parasti `main`).

**CLI alternatīva:**

```bash
copy "RULES.md" .
git add RULES.md
git commit -m "docs: add HomeoGO collaboration rules v1.1"
git push origin main
```

