package com.wh01s17.launcher.core.system.notifications

import android.app.ActivityOptions
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import com.wh01s17.launcher.core.system.media.WhNotificationListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Las notificaciones de otras apps (plan.md §5.8, `NotificationListenerService`) y lo que se puede
 * hacer con ellas. Sin el acceso que da el usuario, [state] trae `access = false` y ninguna
 * notificación: el launcher funciona igual, sólo faltan las insignias, el contador y el panel.
 */
class NotificationRepository(
    context: Context,
    private val hub: NotificationHub = NotificationHub,
) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(NotificationManager::class.java)
    private val listener = ComponentName(appContext, WhNotificationListener::class.java)

    val state: Flow<NotificationState> =
        combine(hub.connected, hub.items) { connected, items -> NotificationState(connected || accessGranted(), items) }
            .distinctUntilChanged()

    private fun accessGranted() = manager.isNotificationListenerAccessGranted(listener)

    /**
     * Con el acceso dado pero el servicio sin enlazar (tras reinstalar la app o si el sistema lo soltó),
     * pide a Android que lo enlace otra vez. No hace nada si falta el acceso.
     */
    fun ensureBound() {
        if (accessGranted() && !hub.connected.value) NotificationListenerService.requestRebind(listener)
    }

    /** Abre la notificación como si se tocara en la cortina; las que se quitan solas, se quitan. */
    fun open(item: NotificationItem): Boolean {
        val intent = item.open ?: return false
        val sent = send(intent)
        if (sent && item.autoCancel && item.clearable) hub.cancel(item.key)
        return sent
    }

    fun dismiss(item: NotificationItem) {
        if (item.clearable) hub.cancel(item.key)
    }

    /** Quita todas las que el usuario puede descartar. */
    fun dismissAll() {
        hub.cancelAll(
            hub.items.value
                .filter { it.clearable }
                .map { it.key },
        )
    }

    /** La pantalla del sistema donde se da el acceso a notificaciones. */
    fun accessSettings(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun send(intent: PendingIntent): Boolean =
        try {
            // Android 14 pide a quien envía un PendingIntent que permita al destino abrir su pantalla.
            val options =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ActivityOptions
                        .makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                        .toBundle()
                } else {
                    null
                }
            intent.send(appContext, 0, null, null, null, null, options)
            true
        } catch (ignored: PendingIntent.CanceledException) {
            false
        }
}
