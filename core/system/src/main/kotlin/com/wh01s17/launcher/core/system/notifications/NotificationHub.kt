package com.wh01s17.launcher.core.system.notifications

import android.service.notification.NotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * El puente entre [com.wh01s17.launcher.core.system.media.WhNotificationListener], que crea Android
 * sin dejarnos pasarle nada, y el resto de la app. Es el único estado global del proceso: el
 * servicio lo escribe y [NotificationRepository] lo lee.
 */
object NotificationHub {
    private val itemsFlow = MutableStateFlow<List<NotificationItem>>(emptyList())
    private val connectedFlow = MutableStateFlow(false)

    @Volatile
    private var service: NotificationListenerService? = null

    val items: StateFlow<List<NotificationItem>> = itemsFlow.asStateFlow()

    /** El servicio está enlazado: Android nos da las notificaciones. */
    val connected: StateFlow<Boolean> = connectedFlow.asStateFlow()

    internal fun connect(
        listener: NotificationListenerService,
        current: List<NotificationItem>,
    ) {
        service = listener
        itemsFlow.value = current
        connectedFlow.value = true
    }

    internal fun update(current: List<NotificationItem>) {
        itemsFlow.value = current
    }

    internal fun disconnect(listener: NotificationListenerService) {
        if (service !== listener) return
        service = null
        itemsFlow.value = emptyList()
        connectedFlow.value = false
    }

    internal fun cancel(key: String): Boolean {
        val listener = service ?: return false
        return try {
            listener.cancelNotification(key)
            true
        } catch (ignored: SecurityException) {
            false
        }
    }

    internal fun cancelAll(keys: List<String>): Boolean {
        val listener = service ?: return false
        return try {
            listener.cancelNotifications(keys.toTypedArray())
            true
        } catch (ignored: SecurityException) {
            false
        }
    }
}
