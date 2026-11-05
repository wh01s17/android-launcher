package com.wh01s17.launcher.feature.widgets.native

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.wh01s17.launcher.core.designsystem.layout.WidgetSize
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
 * Pomodoro (plan.md §7 Hito 7, `docs/pomodoro.md`) en el tema nova: encabezado con la sesión
 * («1/4»), fase y cuenta atrás en su color con su avance, y al pie la banda con iniciar/pausar,
 * saltar, reiniciar, ±1 min y el sistema (tocar pasa al siguiente: 40/10, 25/5, 50/10, 90/20).
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
    val palette = WhTheme.palette
    BoxWithConstraints(modifier.fillMaxSize().background(WhTheme.colors.background).padding(WhTheme.spacing.controlGap)) {
        // Pequeño: cuenta atrás y la banda principal; mediano: además encabezado y avance; grande: todo.
        val size = WidgetSize.of(maxWidth, maxHeight)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
            if (size != WidgetSize.Small) {
                NovaHeading(
                    stringResource(R.string.native_pomodoro_title),
                    stringResource(R.string.native_pomodoro_session, state.sessions + 1, state.preset.sessionsPerLongBreak),
                )
            }
            // En grande, la lectura queda centrada entre el encabezado y las teclas.
            if (size >= WidgetSize.Large) Spacer(Modifier.weight(1f))
            if (size >= WidgetSize.Large) WhText(phaseLabel(state.phase), color = color, maxLines = 1)
            WhText(clock(remaining), style = readoutStyle(size), color = color, maxLines = 1)
            if (size != WidgetSize.Small) NovaMeter(1f - remaining.toFloat() / state.totalMillis, color)
            Spacer(Modifier.weight(1f))
            NovaBand(palette) {
                NovaKey(stringResource(R.string.native_pomodoro_toggle), pomodoro::toggle, palette, interactive, lit = state.running) {
                    TransportGlyph(if (state.running) Transport.Pause else Transport.Play, it)
                }
                NovaKey(
                    stringResource(R.string.native_pomodoro_skip),
                    pomodoro::skip,
                    palette,
                    interactive,
                ) { TransportGlyph(Transport.Next, it) }
                NovaKey(
                    stringResource(R.string.native_pomodoro_reset),
                    pomodoro::reset,
                    palette,
                    interactive,
                ) { WhIcon(WhIcons.Refresh, tint = it) }
            }
            if (size >= WidgetSize.Large) {
                NovaBand(palette) {
                    NovaKey(
                        stringResource(R.string.native_pomodoro_less),
                        { pomodoro.adjust(-1) },
                        palette,
                        interactive,
                    ) { WhText("−1", color = it) }
                    NovaKey(
                        stringResource(R.string.native_pomodoro_preset),
                        { pomodoro.setPreset(nextPreset(state.preset)) },
                        palette,
                        interactive,
                    ) {
                        WhText(presetLabel(state.preset), color = it, maxLines = 1)
                    }
                    NovaKey(
                        stringResource(R.string.native_pomodoro_more),
                        { pomodoro.adjust(1) },
                        palette,
                        interactive,
                    ) { WhText("+1", color = it) }
                }
            }
        }
    }
}

/** «40/10», como el resumen de sistema de `pomodoro.sh`. */
private fun presetLabel(preset: PomodoroPreset) = "${preset.workMinutes}/${preset.shortBreakMinutes}"

private fun nextPreset(preset: PomodoroPreset) = PomodoroPreset.entries[(preset.ordinal + 1) % PomodoroPreset.entries.size]

private const val TICK_MILLIS = 1000L
