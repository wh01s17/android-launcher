package com.wh01s17.launcher.timer

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wh01s17.launcher.MainActivity
import com.wh01s17.launcher.R
import com.wh01s17.launcher.core.system.timer.MetronomeSettings
import com.wh01s17.launcher.core.system.timer.PomodoroPhase
import com.wh01s17.launcher.core.system.timer.PomodoroState
import com.wh01s17.launcher.core.system.timer.TimerSnapshot

/**
 * Las notificaciones del pomodoro y el metrónomo (plan.md §7 Hito 7): la del servicio en primer
 * plano, silenciosa, con la cuenta atrás de la fase y sus botones, y el aviso al terminar una fase,
 * con sonido, como `notify_phase_finished` de `pomodoro.sh`. Sin el permiso de notificaciones
 * (opt-in, R8) el servicio funciona igual y el aviso no se publica.
 */
internal object TimerNotifications {
    const val ONGOING_ID = 0x5717
    private const val ALERT_ID = 0x5718
    private const val CHANNEL_ONGOING = "timers"
    private const val CHANNEL_ALERTS = "pomodoro-alerts"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ONGOING,
                context.getString(R.string.timer_channel_ongoing),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, context.getString(R.string.timer_channel_alerts), NotificationManager.IMPORTANCE_HIGH),
        )
    }

    /** Se pueden publicar: el permiso (Android 13+) y las notificaciones de la app encendidas. */
    fun allowed(context: Context): Boolean {
        val permitted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** La del servicio: el pomodoro (con su cuenta atrás) y/o el metrónomo, con sus botones. */
    fun ongoing(
        context: Context,
        snapshot: TimerSnapshot,
    ): Notification {
        val pomodoro = snapshot.pomodoro
        val metronome = snapshot.metronome
        val builder =
            NotificationCompat
                .Builder(context, CHANNEL_ONGOING)
                .setSmallIcon(R.drawable.ic_stat_wh01s17)
                .setOngoing(true)
                .setSilent(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(openLauncher(context))
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (pomodoro != null) {
            builder
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setContentTitle(context.getString(R.string.timer_pomodoro_title, context.getString(phaseLabel(pomodoro.phase))))
                .setContentText(pomodoroText(context, pomodoro, metronome))
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
                .setWhen(pomodoro.endsAtMillis ?: System.currentTimeMillis())
                .addAction(
                    0,
                    context.getString(R.string.timer_action_pause),
                    TimerService.action(context, TimerService.ACTION_POMODORO_TOGGLE),
                ).addAction(
                    0,
                    context.getString(R.string.timer_action_skip),
                    TimerService.action(context, TimerService.ACTION_POMODORO_SKIP),
                )
        } else if (metronome != null) {
            builder
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentTitle(context.getString(R.string.timer_metronome_title))
                .setContentText(metronome.signature)
                .setShowWhen(false)
        }
        if (metronome != null) {
            builder.addAction(
                0,
                context.getString(R.string.timer_action_stop_metronome),
                TimerService.action(context, TimerService.ACTION_METRONOME_STOP),
            )
        }
        return builder.build()
    }

    /** El aviso de fin de fase: qué terminó y qué empieza. Sin permiso no se publica. */
    fun alertPhaseFinished(
        context: Context,
        finished: PomodoroPhase,
        next: PomodoroState,
    ) {
        if (!allowed(context)) return
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_stat_wh01s17)
                .setContentTitle(context.getString(finishedLabel(finished)))
                .setContentText(
                    context.getString(
                        R.string.timer_alert_next,
                        context.getString(phaseLabel(next.phase)),
                        next.preset.minutes(next.phase),
                    ),
                ).setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openLauncher(context))
                .build()
        try {
            NotificationManagerCompat.from(context).notify(ALERT_ID, notification)
        } catch (ignored: SecurityException) {
            // El permiso se retiró entre la comprobación y el aviso: no hay a quién avisar.
        }
    }

    private fun pomodoroText(
        context: Context,
        pomodoro: PomodoroState,
        metronome: MetronomeSettings?,
    ): String {
        val session = context.getString(R.string.timer_pomodoro_session, pomodoro.sessions + 1, pomodoro.preset.sessionsPerLongBreak)
        return if (metronome == null) session else context.getString(R.string.timer_pomodoro_with_metronome, session, metronome.bpm)
    }

    private fun openLauncher(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    @StringRes
    private fun phaseLabel(phase: PomodoroPhase): Int =
        when (phase) {
            PomodoroPhase.Work -> R.string.timer_phase_work
            PomodoroPhase.ShortBreak -> R.string.timer_phase_short
            PomodoroPhase.LongBreak -> R.string.timer_phase_long
        }

    @StringRes
    private fun finishedLabel(phase: PomodoroPhase): Int =
        if (phase == PomodoroPhase.Work) R.string.timer_alert_work_done else R.string.timer_alert_break_done
}
