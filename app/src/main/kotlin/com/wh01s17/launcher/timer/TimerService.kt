package com.wh01s17.launcher.timer

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.wh01s17.launcher.LauncherApplication
import com.wh01s17.launcher.core.system.timer.TimerSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * El servicio en primer plano del pomodoro y el metrónomo (plan.md §7 Hito 7): mantiene vivo el
 * proceso mientras algo corre (sin él, HyperOS cortaba el metrónomo al salir del launcher) y pinta
 * su notificación. Sólo existe mientras hay algo activo: al pararse todo, se quita solo.
 *
 * Lo arranca [TimerCoordinator]; los botones de la notificación llegan como acciones.
 */
class TimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val container get() = (application as LauncherApplication).container

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        TimerNotifications.createChannels(this)
        val pomodoro = container.pomodoro
        val metronome = container.metronome
        scope.launch {
            combine(pomodoro.state, metronome.state, TimerSnapshot::of).distinctUntilChanged().collect(::foreground)
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_POMODORO_TOGGLE -> container.pomodoro.toggle()
            ACTION_POMODORO_SKIP -> container.pomodoro.skip()
            ACTION_METRONOME_STOP -> container.metronome.stop()
        }
        // Tras `startForegroundService` hay que pasar a primer plano siempre, aunque ya no quede
        // nada que mostrar: si no, Android cierra la app. Después, [show] decide si se queda.
        foreground(TimerSnapshot.of(container.pomodoro.state.value, container.metronome.state.value))
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun foreground(snapshot: TimerSnapshot) {
        ServiceCompat.startForeground(this, TimerNotifications.ONGOING_ID, TimerNotifications.ongoing(this, snapshot), types(snapshot))
        if (!snapshot.active) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    companion object {
        const val ACTION_POMODORO_TOGGLE = "com.wh01s17.launcher.timer.POMODORO_TOGGLE"
        const val ACTION_POMODORO_SKIP = "com.wh01s17.launcher.timer.POMODORO_SKIP"
        const val ACTION_METRONOME_STOP = "com.wh01s17.launcher.timer.METRONOME_STOP"

        /**
         * Tipo de servicio: el metrónomo reproduce audio (`mediaPlayback`); el pomodoro no tiene
         * tipo propio en Android y va como `specialUse` (justificado en el manifiesto). Antes de
         * Android 14 `specialUse` no existe ni hace falta.
         */
        internal fun types(snapshot: TimerSnapshot): Int {
            var types = 0
            if (snapshot.metronome != null) types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            val special = snapshot.pomodoro != null || types == 0
            if (special && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            }
            return types
        }

        internal fun action(
            context: Context,
            action: String,
        ): PendingIntent =
            PendingIntent.getService(
                context,
                action.hashCode(),
                Intent(context, TimerService::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
    }
}
