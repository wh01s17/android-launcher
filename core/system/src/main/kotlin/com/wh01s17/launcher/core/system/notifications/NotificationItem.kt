package com.wh01s17.launcher.core.system.notifications

import android.app.PendingIntent

/**
 * Una notificación de otra app, ya filtrada (ver [NotificationFilter]). [key] es la de Android
 * (`StatusBarNotification.getKey`) y sirve para descartarla.
 */
data class NotificationItem(
    val key: String,
    val packageName: String,
    /** Número de serie del perfil, el mismo que usa `AppKey.userSerial`. */
    val userSerial: Long,
    val title: String?,
    val text: String?,
    val postTime: Long,
    /** El usuario puede descartarla (las fijas, no). */
    val clearable: Boolean,
    /** Android la quita sola al tocarla. */
    val autoCancel: Boolean,
    val open: PendingIntent?,
)

/** La app (y el perfil) a la que pertenece un contador: lo que un tile necesita para saber el suyo. */
data class BadgeKey(
    val packageName: String,
    val userSerial: Long,
)

/** Resumen para la barra: si hay acceso y cuántas notificaciones hay. */
data class NotificationSummary(
    val granted: Boolean = false,
    val count: Int = 0,
)

/**
 * Lo que ve el launcher de las notificaciones. [access] es `false` mientras el usuario no haya dado
 * el acceso a notificaciones en los ajustes de Android (opt-in, plan.md R8).
 */
data class NotificationState(
    val access: Boolean = false,
    val items: List<NotificationItem> = emptyList(),
) {
    val summary: NotificationSummary get() = NotificationSummary(access, items.size)

    /** Cuántas notificaciones tiene cada app, para las insignias de los tiles. */
    val badges: Map<BadgeKey, Int>
        get() = items.groupingBy { BadgeKey(it.packageName, it.userSerial) }.eachCount()
}
