package com.wh01s17.launcher.core.system.notifications

import android.app.Notification

/**
 * Qué notificaciones enseña el launcher. Se deja fuera lo que no es un aviso para el usuario: las
 * propias (el servicio del pomodoro), los resúmenes de grupo (la app ya publica cada una), las fijas
 * de servicios en curso y las que no dicen nada.
 *
 * La música ([media], reconocida por su sesión) se enseña sólo mientras suena: las apps la marcan
 * en curso (`FLAG_ONGOING_EVENT`) al reproducir y le quitan la marca al pausar.
 */
object NotificationFilter {
    fun accepts(
        packageName: String,
        ownPackage: String,
        flags: Int,
        title: CharSequence?,
        text: CharSequence?,
        media: Boolean = false,
    ): Boolean {
        if (packageName == ownPackage) return false
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        val ongoing = flags and Notification.FLAG_ONGOING_EVENT != 0
        if (media) return ongoing
        if (ongoing) return false
        return !title.isNullOrBlank() || !text.isNullOrBlank()
    }
}
