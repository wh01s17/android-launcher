package com.wh01s17.launcher.feature.widgets.native

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wh01s17.launcher.core.designsystem.components.WhIcon
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.timer.PomodoroPhase
import com.wh01s17.launcher.core.system.timer.PomodoroPreset
import com.wh01s17.launcher.core.system.timer.PomodoroRepository
import com.wh01s17.launcher.feature.widgets.R
import kotlinx.coroutines.delay

/** Colores de las fases (`docs/pomodoro.md`): trabajo rojo, descanso corto verde, largo cian. */
@Composable
internal fun phaseColor(phase: PomodoroPhase): Color =
    when (phase) {
        PomodoroPhase.Work -> WhTheme.colors.red
        PomodoroPhase.ShortBreak -> WhTheme.colors.accent
        PomodoroPhase.LongBreak -> WhTheme.colors.cyan
    }

@Composable
internal fun phaseLabel(phase: PomodoroPhase): String =
    stringResource(
        when (phase) {
            PomodoroPhase.Work -> R.string.native_pomodoro_work
            PomodoroPhase.ShortBreak -> R.string.native_pomodoro_short
            PomodoroPhase.LongBreak -> R.string.native_pomodoro_long
        },
    )

/**
 * Pomodoro (plan.md §7 Hito 7, `docs/pomodoro.md`): fase, cuenta atrás y progreso en su color,
 * teclas iniciar/pausar, saltar, reiniciar y ±1 min, y el sistema (tocar cambia al siguiente).
 */
@Composable
internal fun PomodoroWidget(
    pomodoro: PomodoroRepository,
    interactive: Boolean,
    modifier: Modifier = Modifier,
) {
    val state by pomodoro.state.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.running) {
        while (state.running) {
            now = System.currentTimeMillis()
            delay(TICK_MILLIS)
        }
        now = System.currentTimeMillis()
    }
    val remaining = state.remainingAt(now)
    val color = phaseColor(state.phase)
    Column(
        modifier.fillMaxSize().background(WhTheme.colors.background).padding(WhTheme.spacing.controlGap),
        // Arriba, pegado al encabezado, como el widget de TIDAL.
        verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap),
    ) {
        NovaHeading(stringResource(R.string.native_pomodoro_title), presetLabel(state.preset))
        WhText(phaseLabel(state.phase), color = color, maxLines = 1)
        WhText(clock(remaining), style = WhTheme.typography.displayLarge, color = color, maxLines = 1)
        RetroMeter(
            stringResource(R.string.native_pomodoro_session, state.sessions + 1, state.preset.sessionsPerLongBreak),
            1f - remaining.toFloat() / state.totalMillis,
            presetLabel(state.preset),
            color = color,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
            val key = Modifier.weight(1f)
            RetroKey(stringResource(R.string.native_pomodoro_toggle), pomodoro::toggle, key, interactive, lit = state.running) {
                WhIcon(if (state.running) WhIcons.Pause else WhIcons.Play)
            }
            RetroKey(stringResource(R.string.native_pomodoro_skip), pomodoro::skip, key, interactive) { WhIcon(WhIcons.SkipNext) }
            RetroKey(stringResource(R.string.native_pomodoro_reset), pomodoro::reset, key, interactive) { WhIcon(WhIcons.Refresh) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
            val key = Modifier.weight(1f)
            RetroKey(stringResource(R.string.native_pomodoro_less), { pomodoro.adjust(-1) }, key, interactive) { WhText("−1") }
            RetroKey(stringResource(R.string.native_pomodoro_preset), { pomodoro.setPreset(nextPreset(state.preset)) }, key, interactive) {
                WhText(presetLabel(state.preset), maxLines = 1)
            }
            RetroKey(stringResource(R.string.native_pomodoro_more), { pomodoro.adjust(1) }, key, interactive) { WhText("+1") }
        }
    }
}

/** «40/10», como el resumen de sistema de `pomodoro.sh`. */
private fun presetLabel(preset: PomodoroPreset) = "${preset.workMinutes}/${preset.shortBreakMinutes}"

private fun nextPreset(preset: PomodoroPreset) = PomodoroPreset.entries[(preset.ordinal + 1) % PomodoroPreset.entries.size]

private const val TICK_MILLIS = 1000L
