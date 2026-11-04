package com.wh01s17.launcher.bar

import android.content.Context

/**
 * Avisos que se enseñan una sola vez (plan.md §7, Hito 3: «lo indica una vez»). Se recuerda en un
 * `SharedPreferences` propio: es un dato de UI, no de la config del usuario.
 */
class OneTimeHints(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** `true` la primera vez que se pregunta por [key]; después, `false`. */
    fun shouldShow(key: String): Boolean {
        if (prefs.getBoolean(key, false)) return false
        prefs.edit().putBoolean(key, true).apply()
        return true
    }

    private companion object {
        const val FILE = "one-time-hints"
    }
}
