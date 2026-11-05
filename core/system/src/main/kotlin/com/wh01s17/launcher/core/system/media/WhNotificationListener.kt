package com.wh01s17.launcher.core.system.media

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.wh01s17.launcher.core.system.notifications.NotificationHub
import com.wh01s17.launcher.core.system.notifications.toItems

/**
 * El acceso a notificaciones (opt-in, plan.md R8). Android lo exige para dos cosas: ver las
 * sesiones multimedia de otras apps ([MediaSource]) y leer las notificaciones para las insignias de
 * los tiles, el contador de la barra y el panel. Lo que lee sólo pasa por [NotificationHub]; no se
 * guarda ni sale del proceso.
 */
class WhNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() = NotificationHub.connect(this, active())

    override fun onListenerDisconnected() = NotificationHub.disconnect(this)

    override fun onDestroy() {
        NotificationHub.disconnect(this)
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = NotificationHub.update(active())

    override fun onNotificationRemoved(sbn: StatusBarNotification) = NotificationHub.update(active())

    /** Las notificaciones de ahora; Android puede rechazar la lectura si el servicio ya se está soltando. */
    private fun active() =
        try {
            activeNotifications.orEmpty().toList().toItems(this)
        } catch (ignored: SecurityException) {
            emptyList()
        }
}
