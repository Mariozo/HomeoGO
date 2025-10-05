// File: app/src/main/java/lv/mariozo/homeogo/logic/ElzaLogicManager.kt
// Project: HomeoGO
// Created: 04.okt.2025 13:40 (Rīga)
// ver. 1.8
// Purpose: AI-first conversation gateway for Elza (text-in → text-out).
//          Uses a pluggable AI port when online; falls back to OfflineResponder when offline.
// Notes:
//  - Pure orchestration. No SDK atslēgas, nav tīkla koda. Nekādu “ierakstītu” frāžu, izņemot skaidru offline ziņu.
//  - Inject AI via constructor: ai = { text, locale -> /* tavs backend izsaukums; atgriež LV atbildi */ }.
//  - Inject connectivity via isOnline(): Boolean. Ja false → izmanto OfflineResponder.
//  - Garantijas: ne-eho (nekad neatkārto lietotāja teikto 1:1), apgriež un pievieno pieturzīmi. LV pēc noklusējuma.

package lv.mariozo.homeogo.logic

// # 1. ---- Public API ----------------------------------------------------------
data class ElzaResponse(val text: String, val locale: String = "lv-LV")

/** Port uz jebkuru AI/LLM vai tavu backend — jāatgriež īsa, sakarīga LV atbilde. */
typealias ElzaAiPort = suspend (userText: String, locale: String) -> String

// # 2. ---- Logic Manager -------------------------------------------------------
class ElzaLogicManager(
    private val ai: ElzaAiPort? = null,
    private val isOnline: () -> Boolean = { true },
    private val defaultLocale: String = "lv-LV",
) {

    // # 2.1 ---- Main entry -----------------------------------------------------
    suspend fun replyTo(prompt: String, locale: String = defaultLocale): ElzaResponse {
        val user = prompt.normalize()
        if (user.isEmpty()) return ElzaResponse("Nesadzirdēju. Vari atkārtot?", locale)

        // Offline → skaidrs, lietišķs paziņojums
        if (!isOnline()) {
            val text = OfflineResponder.message(user, locale).cleanAgainst(user).ensurePunctuation()
            return ElzaResponse(text, locale)
        }

        // Online + AI → izmanto AI portu
        ai?.let { port ->
            val raw = runCatching { port(user, locale) }.getOrNull().orEmpty().trim()
            val safe = raw.cleanAgainst(user).fallbackIfBlank(locale).ensurePunctuation()
            return ElzaResponse(safe, locale)
        }

        // Online, bet AI nav konfigurēts → godīgs, minimāls paziņojums
        val minimal = minimalOnlineFallback(user, locale).ensurePunctuation()
        return ElzaResponse(minimal, locale)
    }

    // # 2.2 ---- Online fallback (nav AI) --------------------------------------
    private fun minimalOnlineFallback(user: String, locale: String): String {
        val isQ = user.endsWith("?")
        return if (locale.equals("lv-LV", true)) {
            if (isQ) "Domāšanas motors nav pieslēgts. Kad tas būs aktivizēts, došu pilnvērtīgu atbildi."
            else "Sapratu. Kad pieslēgsim domāšanas motoru, varēšu atbildēt saprātīgi."
        } else {
            if (isQ) "Reasoning engine not configured. Once enabled, I’ll provide a full answer."
            else "Got it. I’ll answer intelligently once the reasoning engine is connected."
        }
    }
}

// # 3. ---- Helpers -------------------------------------------------------------
private fun String.normalize(): String = trim().replace(Regex("\\s+"), " ")

private fun String.cleanAgainst(user: String): String =
    when {
        isBlank() -> ""
        equals(user.trim(), ignoreCase = true) -> "" // avoid echo
        else -> this
    }

private fun String.fallbackIfBlank(locale: String): String =
    if (isBlank())
        if (locale.equals(
                "lv-LV",
                true
            )
        ) "Nevaru atbildēt šobrīd." else "I cannot answer right now."
    else this

private fun String.ensurePunctuation(): String =
    if (isEmpty() || last() in ".!?…") this else this + "."
