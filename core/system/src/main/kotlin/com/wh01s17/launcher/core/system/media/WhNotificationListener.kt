package com.wh01s17.launcher.core.system.media

import android.service.notification.NotificationListenerService

/**
 * Existe para que el usuario pueda darle a wh01s17 el acceso a notificaciones, que es lo que
 * Android pide para ver las sesiones multimedia de otras apps ([MediaSource]). No lee ninguna
 * notificación todavía (los contadores llegan con el Hito 8).
 */
class WhNotificationListener : NotificationListenerService()
