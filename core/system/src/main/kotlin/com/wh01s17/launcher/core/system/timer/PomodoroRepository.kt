package com.wh01s17.launcher.core.system.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.Properties

/**
 * El pomodoro del launcher (plan.md §7 Hito 7): un único estado para el tile, la barra y el
 * servicio en primer plano. Avanza solo al terminar cada fase y se guarda en [file] en cada cambio,
 * para seguir donde estaba si Android cierra el proceso. [clock] es la hora de pared en ms.
 */
class PomodoroRepository(
    private val file: File,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutableState = MutableStateFlow(load())
    val state: StateFlow<PomodoroState> = mutableState.asStateFlow()

    private var ticker: Job? = null

    init {
        update { Pomodoro.tick(it, clock()) }
    }

    fun toggle() = update { Pomodoro.toggle(it, clock()) }

    fun skip() = update { Pomodoro.skip(it, clock()) }

    fun reset() = update(Pomodoro::reset)

    fun setPreset(preset: PomodoroPreset) = update { Pomodoro.setPreset(preset) }

    fun adjust(minutes: Int) = update { Pomodoro.adjust(it, minutes, clock()) }

    /**
     * Avanza si la fase ya terminó. Lo llama la alarma exacta del fin de fase: con el teléfono
     * dormido, el temporizador interno ([schedule]) puede llegar minutos tarde.
     */
    fun tick() = update { Pomodoro.tick(it, clock()) }

    private fun update(transform: (PomodoroState) -> PomodoroState) {
        val next = transform(mutableState.value)
        if (next != mutableState.value) {
            mutableState.value = next
            save(next)
        }
        schedule(next)
    }

    /** Despierta justo al terminar la fase; la UI calcula los segundos con [PomodoroState.remainingAt]. */
    private fun schedule(state: PomodoroState) {
        ticker?.cancel()
        if (!state.running) return
        ticker =
            scope.launch {
                delay(state.remainingAt(clock()))
                update { Pomodoro.tick(it, clock()) }
            }
    }

    private fun load(): PomodoroState {
        val p =
            try {
                file.takeIf { it.isFile }?.inputStream()?.use { Properties().apply { load(it) } }
            } catch (ignored: IOException) {
                null
            } catch (ignored: IllegalArgumentException) {
                null
            } ?: return PomodoroState()
        val preset = PomodoroPreset.fromConfig(p.getProperty(PRESET).orEmpty()) ?: PomodoroPreset.Balanced
        val phase = PomodoroPhase.entries.firstOrNull { it.name == p.getProperty(PHASE) } ?: PomodoroPhase.Work
        val ends = p.getProperty(ENDS)?.toLongOrNull()
        return PomodoroState(
            preset = preset,
            phase = phase,
            running = ends != null,
            remainingMillis = p.getProperty(REMAINING)?.toLongOrNull() ?: preset.minutes(phase) * PomodoroState.MILLIS_PER_MINUTE,
            endsAtMillis = ends,
            sessions = p.getProperty(SESSIONS)?.toIntOrNull() ?: 0,
        )
    }

    private fun save(state: PomodoroState) {
        val p =
            Properties().apply {
                setProperty(PRESET, state.preset.configName)
                setProperty(PHASE, state.phase.name)
                setProperty(REMAINING, state.remainingMillis.toString())
                setProperty(SESSIONS, state.sessions.toString())
                state.endsAtMillis?.let { setProperty(ENDS, it.toString()) }
            }
        try {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.outputStream().use { p.store(it, null) }
            temp.renameTo(file)
        } catch (ignored: IOException) {
            // Sin disco el pomodoro sigue en memoria; sólo se perdería si Android cierra el proceso.
        }
    }

    private companion object {
        const val PRESET = "preset"
        const val PHASE = "phase"
        const val REMAINING = "remaining"
        const val ENDS = "endsAt"
        const val SESSIONS = "sessions"
    }
}
