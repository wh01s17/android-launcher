package com.wh01s17.launcher.feature.widgets.native.hack

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.layout.WidgetSize
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.timer.PomodoroPhase
import com.wh01s17.launcher.core.system.timer.PomodoroPreset
import com.wh01s17.launcher.core.system.timer.PomodoroRepository
import com.wh01s17.launcher.feature.widgets.R
import com.wh01s17.launcher.feature.widgets.native.clock
import com.wh01s17.launcher.feature.widgets.native.phaseColor
import com.wh01s17.launcher.feature.widgets.native.phaseLabel
import com.wh01s17.launcher.feature.widgets.native.readoutStyle
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * El pomodoro hack: `focus 40/10`. `[*]` y la fase en su color mientras corre, la cuenta atrás con el
 * cursor (en grande, en cifras de bloques como `tty-clock`), el avance en ASCII y las teclas
 * `[run]`/`[halt]`, `[skip]`, `[kill]`; en grande, la sesión, la hora de fin y `[-1m]`, el sistema y
 * `[+1m]`.
 */
@Composable
internal fun HackPomodoro(
    pomodoro: PomodoroRepository,
    interactive: Boolean,
    modifier: Modifier,
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
    val fraction = if (state.totalMillis > 0) 1f - remaining.toFloat() / state.totalMillis else 0f
    HackFrame(modifier) { size ->
        if (size != WidgetSize.Small) {
            HackPrompt("focus ${preset(state.preset)}")
            val mark = if (state.running) "[*]" else "[ ]"
            WhText("$mark ${phaseLabel(state.phase).uppercase()}", style = WhTheme.typography.bodySmall, color = color, maxLines = 1)
        }
        if (size >= WidgetSize.Large) {
            // En grande, la cuenta atrás en cifras de bloques, como `tty-clock`.
            HackDigits(clock(remaining), color)
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                WhText(clock(remaining), style = readoutStyle(size), color = color, maxLines = 1)
                if (state.running) HackCursor(WhTheme.typography.title)
            }
        }
        if (size != WidgetSize.Small) HackMeter("", fraction, "%3d%%".format((fraction * PERCENT).roundToInt()), color)
        if (size >= WidgetSize.Large) {
            HackCycle(state.phase, state.sessions, state.preset.sessionsPerLongBreak)
            HackLine("session", "${state.sessions + 1}/${state.preset.sessionsPerLongBreak}")
            if (state.running) HackLine("eta", ETA.format(Instant.ofEpochMilli(now + remaining).atZone(ZoneId.systemDefault())))
        }
        Spacer(Modifier.weight(1f))
        HackKeys {
            HackKey(
                if (state.running) "halt" else "run",
                stringResource(R.string.native_pomodoro_toggle),
                pomodoro::toggle,
                interactive,
                lit = state.running,
            )
            HackKey("skip", stringResource(R.string.native_pomodoro_skip), pomodoro::skip, interactive)
            HackKey("kill", stringResource(R.string.native_pomodoro_reset), pomodoro::reset, interactive)
        }
        if (size >= WidgetSize.Large) {
            HackKeys {
                HackKey("-1m", stringResource(R.string.native_pomodoro_less), { pomodoro.adjust(-1) }, interactive)
                HackKey(
                    preset(state.preset),
                    stringResource(R.string.native_pomodoro_preset),
                    { pomodoro.setPreset(PomodoroPreset.entries[(state.preset.ordinal + 1) % PomodoroPreset.entries.size]) },
                    interactive,
                )
                HackKey("+1m", stringResource(R.string.native_pomodoro_more), { pomodoro.adjust(1) }, interactive)
            }
        }
    }
}

/**
 * El ciclo entero, `F b F b F b F B`: enfoque, descanso corto y, al final, el largo (sin corchetes para
 * que quepa en un tile mediano). Lo hecho en acento, la fase actual en vídeo inverso y lo que falta apagado.
 */
@Composable
private fun HackCycle(
    phase: PomodoroPhase,
    sessions: Int,
    perLongBreak: Int,
) {
    val colors = WhTheme.colors
    val slots = perLongBreak * 2
    val current =
        when (phase) {
            PomodoroPhase.Work -> sessions * 2
            PomodoroPhase.ShortBreak -> (sessions * 2 - 1).coerceAtLeast(0)
            PomodoroPhase.LongBreak -> slots - 1
        }
    val text =
        buildAnnotatedString {
            for (slot in 0 until slots) {
                val label =
                    if (slot % 2 == 0) {
                        "F"
                    } else if (slot == slots - 1) {
                        "B"
                    } else {
                        "b"
                    }
                val style =
                    when {
                        slot == current -> SpanStyle(color = colors.background, background = phaseColor(phase))
                        slot < current -> SpanStyle(color = colors.accent)
                        else -> SpanStyle(color = colors.muted)
                    }
                if (slot > 0) append(' ')
                withStyle(style) { append(label) }
            }
        }
    WhText(text, style = WhTheme.typography.bodySmall, maxLines = 1)
}

private fun preset(preset: PomodoroPreset) = "${preset.workMinutes}/${preset.shortBreakMinutes}"

private val ETA = DateTimeFormatter.ofPattern("HH:mm")
private const val TICK_MILLIS = 1000L
private const val PERCENT = 100
