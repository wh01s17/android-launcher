package com.wh01s17.launcher.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.wh01s17.launcher.LauncherApplication
import com.wh01s17.launcher.core.system.timer.MetronomeRepository
import com.wh01s17.launcher.core.system.timer.Pomodoro
import com.wh01s17.launcher.core.system.timer.PomodoroRepository
import com.wh01s17.launcher.core.system.timer.TimerSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Coordina el pomodoro y el metrónomo con Android, mientras vive el proceso del launcher:
 * - arranca [TimerService] cuando algo empieza a correr (y lo reintenta al volver al inicio, por si
 *   Android no dejó arrancarlo desde segundo plano);
 * - programa una alarma exacta para el fin de la fase: dormido, el temporizador interno se retrasa;
 * - publica el aviso cuando una fase termina sola;
 * - la primera vez que algo corre sin permiso de notificaciones, pide explicarlo ([askNotifications]).
 */
class TimerCoordinator(
    private val context: Context,
    private val pomodoro: PomodoroRepository,
    private val metronome: MetronomeRepository,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val mutableAsk = MutableStateFlow(false)

    /** Mostrar la explicación del permiso de notificaciones (opt-in, plan.md R8). */
    val askNotifications: StateFlow<Boolean> = mutableAsk.asStateFlow()

    private val snapshots = combine(pomodoro.state, metronome.state, TimerSnapshot::of).distinctUntilChanged()

    fun start() {
        TimerNotifications.createChannels(context)
        scope.launch {
            snapshots.map { it.active }.distinctUntilChanged().collect { active ->
                if (active) {
                    startService()
                    if (!TimerNotifications.allowed(context) && !prefs.getBoolean(DECLINED, false)) mutableAsk.value = true
                }
            }
        }
        scope.launch { pomodoro.state.collect { scheduleAlarm(it.endsAtMillis.takeIf { _ -> it.running }) } }
        scope.launch {
            var previous = pomodoro.state.value
            pomodoro.state.collect { next ->
                Pomodoro.finished(previous, next, clock())?.let { TimerNotifications.alertPhaseFinished(context, it, next) }
                previous = next
            }
        }
    }

    /** Al volver al inicio: si algo corre y el servicio no pudo arrancar en segundo plano, ahora sí. */
    fun ensureService() {
        if (TimerSnapshot.of(pomodoro.state.value, metronome.state.value).active) startService()
    }

    /** La explicación se respondió: con «ahora no» no se vuelve a preguntar (se activa en Android). */
    fun notificationsAnswered(declined: Boolean) {
        if (declined) prefs.edit().putBoolean(DECLINED, true).apply()
        mutableAsk.value = false
    }

    private fun startService() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
        } catch (ignored: IllegalStateException) {
            // Android 12+ no deja arrancarlo desde segundo plano; se reintenta en [ensureService].
        }
    }

    private fun scheduleAlarm(endsAt: Long?) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, PomodoroAlarmReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        if (endsAt == null) {
            alarms.cancel(intent)
            return
        }
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exact) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, intent)
        }
    }

    private companion object {
        const val PREFS = "timers"
        const val DECLINED = "notifications-declined"
    }
}

/** La alarma del fin de fase: despierta el proceso y avanza el pomodoro. */
class PomodoroAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        (context.applicationContext as LauncherApplication).container.pomodoro.tick()
    }
}
