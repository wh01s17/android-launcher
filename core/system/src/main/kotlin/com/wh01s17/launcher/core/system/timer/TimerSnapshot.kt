package com.wh01s17.launcher.core.system.timer

/**
 * Lo que muestra la notificación de los temporizadores: el pomodoro si corre y los ajustes del
 * metrónomo si suena; `null` lo que está parado. No incluye el tiempo actual del metrónomo, así
 * que dos instantáneas seguidas sólo difieren cuando hay algo nuevo que enseñar (no en cada tiempo).
 */
data class TimerSnapshot(
    val pomodoro: PomodoroState? = null,
    val metronome: MetronomeSettings? = null,
) {
    /** Algo corre: hace falta el servicio en primer plano (plan.md §7 Hito 7). */
    val active: Boolean get() = pomodoro != null || metronome != null

    companion object {
        fun of(
            pomodoro: PomodoroState,
            metronome: MetronomeState,
        ): TimerSnapshot = TimerSnapshot(pomodoro.takeIf { it.running }, metronome.settings.takeIf { metronome.running })
    }
}
