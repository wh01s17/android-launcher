package com.wh01s17.launcher.core.system.navigation

import android.content.Context
import android.content.Intent

/**
 * Abre la cortina de notificaciones del sistema (plan.md §3 y §7, Hito 3: deslizar la barra hacia
 * abajo). Un launcher no puede hacerlo por su cuenta; sólo el servicio de accesibilidad
 * (`GLOBAL_ACTION_NOTIFICATIONS`), que vive en otro proceso, así que se le pide con una difusión
 * interna. Sin el servicio activo no se hace nada.
 */
class NotificationShade(
    private val context: Context,
    private val watchdog: NavigationWatchdog,
) {
    /** `true` si se pidió al servicio; `false` si no está activo (el llamador avisa al usuario). */
    fun open(): Boolean {
        if (!watchdog.isNavigationServiceRunning()) return false
        context.sendBroadcast(Intent(action(context)).setPackage(context.packageName))
        return true
    }

    companion object {
        /** Acción de la difusión; el servicio la escucha. */
        fun action(context: Context) = "${context.packageName}.OPEN_NOTIFICATION_SHADE"
    }
}
