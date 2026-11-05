package com.wh01s17.launcher.core.system.timer

/**
 * Sistemas de `pomodoro.sh` de los dotfiles (`docs/pomodoro.md`): minutos de trabajo, descanso y
 * descanso largo, y cada cuántas sesiones toca el largo.
 */
@Suppress("MagicNumber") // Minutos de `pomodoro.sh`, copiados tal cual.
enum class PomodoroPreset(
    val configName: String,
    val workMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    val sessionsPerLongBreak: Int,
) {
    Balanced("balanced", 40, 10, 20, 4),
    Classic("classic", 25, 5, 15, 4),
    Deep("deep", 50, 10, 20, 4),
    Ultradian("ultradian", 90, 20, 30, 2),
    ;

    fun minutes(phase: PomodoroPhase): Int =
        when (phase) {
            PomodoroPhase.Work -> workMinutes
            PomodoroPhase.ShortBreak -> shortBreakMinutes
            PomodoroPhase.LongBreak -> longBreakMinutes
        }

    companion object {
        fun fromConfig(name: String): PomodoroPreset? = entries.firstOrNull { it.configName == name }
    }
}

/** Fases del pomodoro; sus colores son los de `docs/pomodoro.md` (rojo, verde, cian). */
enum class PomodoroPhase { Work, ShortBreak, LongBreak }

/**
 * Estado del pomodoro en un instante. Mientras corre se guarda cuándo termina la fase
 * ([endsAtMillis], en el reloj monotónico), no lo que falta: así no se desfasa aunque la UI no
 * esté mirando. En pausa, [remainingMillis] es lo que faltaba.
 */
data class PomodoroState(
    val preset: PomodoroPreset = PomodoroPreset.Balanced,
    val phase: PomodoroPhase = PomodoroPhase.Work,
    val running: Boolean = false,
    val remainingMillis: Long = preset.minutes(phase) * MILLIS_PER_MINUTE,
    val endsAtMillis: Long? = null,
    /** Sesiones de trabajo terminadas desde el último descanso largo. */
    val sessions: Int = 0,
) {
    fun remainingAt(nowMillis: Long): Long = (endsAtMillis?.let { it - nowMillis } ?: remainingMillis).coerceAtLeast(0)

    val totalMillis: Long get() = preset.minutes(phase) * MILLIS_PER_MINUTE

    companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}

/**
 * Las transiciones del pomodoro, sin Android ni reloj propio (se prueban en la JVM): iniciar/pausar,
 * saltar fase, reiniciar, cambiar de sistema, ajustar ±1 minuto y avanzar cuando se acaba el tiempo.
 * Como `pomodoro.sh`: al terminar el trabajo toca descanso corto, o largo cada N sesiones; tras un
 * descanso vuelve el trabajo, y la fase nueva empieza sola si la anterior corría.
 */
object Pomodoro {
    private val ADJUST_RANGE = PomodoroState.MILLIS_PER_MINUTE..4 * 60 * PomodoroState.MILLIS_PER_MINUTE

    fun toggle(
        state: PomodoroState,
        now: Long,
    ): PomodoroState =
        if (state.running) {
            state.copy(running = false, remainingMillis = state.remainingAt(now), endsAtMillis = null)
        } else {
            state.copy(running = true, endsAtMillis = now + state.remainingMillis)
        }

    fun skip(
        state: PomodoroState,
        now: Long,
    ): PomodoroState = next(state, now)

    fun reset(state: PomodoroState): PomodoroState = PomodoroState(preset = state.preset)

    fun setPreset(preset: PomodoroPreset): PomodoroState = PomodoroState(preset = preset)

    /** ±[minutes] a la fase actual, entre 1 minuto y 4 horas. */
    fun adjust(
        state: PomodoroState,
        minutes: Int,
        now: Long,
    ): PomodoroState {
        val remaining = (state.remainingAt(now) + minutes * PomodoroState.MILLIS_PER_MINUTE).coerceIn(ADJUST_RANGE)
        return if (state.running) state.copy(endsAtMillis = now + remaining) else state.copy(remainingMillis = remaining)
    }

    /**
     * La fase que terminó sola entre [previous] y [next] (para avisar, como `notify_phase_finished`
     * de `pomodoro.sh`), o `null` si el cambio fue del usuario: saltar, pausar o reiniciar antes de
     * tiempo no avisan.
     */
    fun finished(
        previous: PomodoroState,
        next: PomodoroState,
        now: Long,
    ): PomodoroPhase? {
        val endsAt = previous.endsAtMillis ?: return null
        return previous.phase.takeIf { previous.running && now >= endsAt && next != previous }
    }

    /** Avanza las fases que ya terminaron (puede ser más de una si el teléfono estuvo dormido). */
    fun tick(
        state: PomodoroState,
        now: Long,
    ): PomodoroState {
        var current = state
        while (current.running && current.remainingAt(now) == 0L) current = next(current, current.endsAtMillis ?: now)
        return current
    }

    private fun next(
        state: PomodoroState,
        from: Long,
    ): PomodoroState {
        val sessions = if (state.phase == PomodoroPhase.Work) state.sessions + 1 else state.sessions
        val phase =
            when {
                state.phase != PomodoroPhase.Work -> PomodoroPhase.Work
                sessions >= state.preset.sessionsPerLongBreak -> PomodoroPhase.LongBreak
                else -> PomodoroPhase.ShortBreak
            }
        val total = state.preset.minutes(phase) * PomodoroState.MILLIS_PER_MINUTE
        return state.copy(
            phase = phase,
            sessions = if (phase == PomodoroPhase.LongBreak) 0 else sessions,
            remainingMillis = total,
            endsAtMillis = if (state.running) from + total else null,
        )
    }
}
