package com.wh01s17.launcher.feature.bar.timer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wh01s17.launcher.core.designsystem.components.WhIcon
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.icons.WhGlyph
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.timer.PomodoroPhase
import com.wh01s17.launcher.core.system.timer.PomodoroRepository
import com.wh01s17.launcher.core.system.timer.PomodoroState
import com.wh01s17.launcher.feature.bar.R
import com.wh01s17.launcher.feature.bar.module.BarItem
import com.wh01s17.launcher.feature.bar.module.BarModule
import com.wh01s17.launcher.feature.bar.module.BarModuleContext
import com.wh01s17.launcher.feature.bar.module.moduleDefaults
import com.wh01s17.launcher.feature.bar.module.rememberModuleStyle
import kotlinx.coroutines.delay

/**
 * `pomodoro`: el módulo de `pomodoro.sh` sobre el mismo [PomodoroRepository] que el tile. Muestra
 * el icono de la fase y lo que falta; en reposo (parado y sin empezar), sólo el icono, para no
 * ocupar la barra del teléfono. Tocar inicia o pausa (el clic izquierdo de `shell.json`); mantener
 * pulsado abre el panel con saltar, reiniciar, ±1 minuto y el sistema.
 *
 * Clases como las del escritorio: `work-running`, `short-break-paused`… Por defecto, trabajo en
 * rojo, descanso corto en verde y largo en cian; en pausa, atenuado.
 */
internal class PomodoroModule(
    private val pomodoro: PomodoroRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : BarModule {
    override val id = ID

    @Composable
    override fun Content(context: BarModuleContext) {
        val state by pomodoro.state.collectAsStateWithLifecycle()
        val now by produceState(clock(), state) {
            value = clock()
            while (state.running) {
                delay(SECOND - clock() % SECOND)
                value = clock()
            }
        }
        var panelOpen by rememberSaveable { mutableStateOf(false) }
        val colors = WhTheme.colors
        val defaults =
            moduleDefaults(
                classColors =
                    mapOf(
                        "work-running" to colors.red,
                        "short-break-running" to colors.accent,
                        "long-break-running" to colors.cyan,
                    ),
                dimClasses = PomodoroPhase.entries.map { "${PomodoroBarModel.className(it)}-paused" }.toSet(),
            )
        val style = rememberModuleStyle(context.settings, listOf(PomodoroBarModel.classes(state)), defaults)
        val remaining = state.remainingAt(now)
        BarItem(
            style = style,
            contentDescription = stringResource(R.string.bar_pomodoro, PomodoroBarModel.clock(remaining)),
            onClick = pomodoro::toggle,
            onLongClick = { panelOpen = true },
        ) {
            WhIcon(phaseGlyph(state.phase))
            if (!PomodoroBarModel.idle(state, now)) WhText(PomodoroBarModel.clock(remaining), maxLines = 1)
        }
        if (panelOpen) PomodoroPanel(pomodoro, state, remaining, onDismiss = { panelOpen = false })
    }

    companion object {
        const val ID = "pomodoro"
        private const val SECOND = 1000L
    }
}

/** El icono de cada fase: reloj para el trabajo, hoja para el descanso corto, luna para el largo. */
internal fun phaseGlyph(phase: PomodoroPhase): WhGlyph =
    when (phase) {
        PomodoroPhase.Work -> WhIcons.Clock
        PomodoroPhase.ShortBreak -> WhIcons.Leaf
        PomodoroPhase.LongBreak -> WhIcons.Moon
    }

/** Lo que el módulo calcula sin Compose (se prueba en la JVM). */
internal object PomodoroBarModel {
    private const val MILLIS_PER_SECOND = 1000L
    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3600L

    /** `work`, `short-break`, `long-break`: los nombres de fase de `pomodoro.sh`. */
    fun className(phase: PomodoroPhase): String =
        when (phase) {
            PomodoroPhase.Work -> "work"
            PomodoroPhase.ShortBreak -> "short-break"
            PomodoroPhase.LongBreak -> "long-break"
        }

    /** `work-running`, `short-break-paused`… como `class` en la salida de `pomodoro.sh print`. */
    fun classes(state: PomodoroState): String = className(state.phase) + if (state.running) "-running" else "-paused"

    /**
     * Sin empezar: trabajo parado, entero y sin sesiones hechas (lo que deja reiniciar). Un descanso
     * en pausa, aunque esté entero, sí muestra su tiempo: dice en qué fase se quedó.
     */
    fun idle(
        state: PomodoroState,
        now: Long,
    ): Boolean = !state.running && state.phase == PomodoroPhase.Work && state.sessions == 0 && state.remainingAt(now) == state.totalMillis

    /** `m:ss` o `h:mm:ss` (el ultradiano dura 90 minutos), redondeando hacia abajo como el tile. */
    fun clock(millis: Long): String {
        val seconds = millis / MILLIS_PER_SECOND
        val h = seconds / SECONDS_PER_HOUR
        val m = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val s = seconds % SECONDS_PER_MINUTE
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
