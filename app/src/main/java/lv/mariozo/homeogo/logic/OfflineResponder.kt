// File: app/src/main/java/lv/mariozo/homeogo/logic/OfflineResponder.kt
// Project: HomeoGO
// Created: 04.okt.2025 13:10 (Rīga)
// ver. 1.0
// Purpose: Clear, localized messages for offline mode – used when no network/AI is available.
// Notes:
//  - Pure local utilities; no network. Called by ElzaLogicManager when isOnline()==false.
//  - Keeps answers short, explicit that we are offline, and avoids echo.

package lv.mariozo.homeogo.logic

// # 1. ---- API ----------------------------------------------------------------
internal object OfflineResponder {

    /** Returns a concise offline message in the given locale. */
    fun message(userText: String, locale: String): String {
        val lv = locale.equals("lv-LV", true)
        val isQuestion = userText.trim().endsWith("?")

        return if (lv) {
            if (isQuestion)
                "Pašlaik esmu bezsaistē, tāpēc nevaru sniegt pilnu atbildi. Pieslēdzot tīklu, mēģināšu atbildēt."
            else
                "Pašlaik esmu bezsaistē. Kad būs savienojums, varēšu palīdzēt precīzāk."
        } else {
            if (isQuestion)
                "I’m currently offline, so I can’t provide a full answer. Once we’re online, I’ll try to answer."
            else
                "I’m offline right now. When connection is available, I’ll help more precisely."
        }
    }
}
