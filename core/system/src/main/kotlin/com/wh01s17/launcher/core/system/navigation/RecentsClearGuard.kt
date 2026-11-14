package com.wh01s17.launcher.core.system.navigation

/**
 * Al pulsar «Eliminar todo» en los recientes de HyperOS, su pantalla de recientes mantiene y abre la
 * que cree que es la app actual. Esa pantalla ignora las tareas de inicio, así que con wh01s17 como
 * launcher elige la última app usada aunque se viniera del inicio (visto el 2026-11-13 con Instagram).
 *
 * Esta lógica, sin Android, recuerda la última app en primer plano antes de los recientes y, si era
 * el launcher, pide volver al inicio cuando aparece la app que HyperOS mantuvo. Viniendo de una app,
 * HyperOS ya deja esa app y no se hace nada.
 */
class RecentsClearGuard(
    private val launcherPackage: String,
    private val recentsPackage: String = MIUI_HOME,
    private val windowMillis: Long = DEFAULT_WINDOW_MILLIS,
) {
    private var lastApp: String? = null
    private var pendingUntil: Long? = null

    /** Ha cambiado la app de la ventana activa. `true` = hay que ir al inicio ahora. */
    fun onActiveApp(
        packageName: String,
        timeMillis: Long,
    ): Boolean {
        // La propia pantalla de recientes no cuenta como app.
        if (packageName == recentsPackage) return false
        val goHome = packageName != launcherPackage && pendingUntil?.let { timeMillis <= it } == true
        pendingUntil = null
        lastApp = packageName
        return goHome
    }

    /** Tras «Eliminar todo» desde el inicio, aún se espera la app que HyperOS mantenga. */
    fun isPending(timeMillis: Long): Boolean = pendingUntil?.let { timeMillis <= it } == true

    /** Se pulsó «Eliminar todo» en los recientes. */
    fun onClearAll(timeMillis: Long) {
        pendingUntil = if (lastApp == launcherPackage) timeMillis + windowMillis else null
    }

    companion object {
        const val MIUI_HOME = "com.miui.home"

        /** Id del botón «Eliminar todo en Recientes» de HyperOS 2 (`uiautomator dump`). */
        const val CLEAR_ALL_ID = "com.miui.home:id/clearAnimView"

        /** HyperOS abre la app mantenida en menos de un segundo; con margen para la animación. */
        const val DEFAULT_WINDOW_MILLIS = 3_000L
    }
}
