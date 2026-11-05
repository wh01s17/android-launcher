package com.wh01s17.launcher.core.system.notifications

import android.app.Notification

/**
 * Qué notificaciones enseña el launcher. Se deja fuera lo que no es un aviso para el usuario: las
 * propias (el servicio del pomodoro), los resúmenes de grupo (la app ya publica cada una), las fijas
 * de servicios en curso y música (eso es de la barra y los widgets) y las que no dicen nada.
 */
object NotificationFilter {
    fun accepts(
        packageName: String,
        ownPackage: String,
        flags: Int,
        title: CharSequence?,
        text: CharSequence?,
    ): Boolean {
        if (packageName == ownPackage) return false
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return false
        return !title.isNullOrBlank() || !text.isNullOrBlank()
    }
}
