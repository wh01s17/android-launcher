package com.wh01s17.launcher.feature.bar.timer

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.designsystem.components.WhButton
import com.wh01s17.launcher.core.designsystem.components.WhNumberField
import com.wh01s17.launcher.core.designsystem.components.WhPanel
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.timer.MetronomeMath
import com.wh01s17.launcher.core.system.timer.MetronomeRepository
import com.wh01s17.launcher.core.system.timer.MetronomeState
import com.wh01s17.launcher.core.system.timer.PomodoroPhase
import com.wh01s17.launcher.core.system.timer.PomodoroPreset
import com.wh01s17.launcher.core.system.timer.PomodoroRepository
import com.wh01s17.launcher.core.system.timer.PomodoroState
import com.wh01s17.launcher.core.system.timer.Subdivision
import com.wh01s17.launcher.feature.bar.BarPopup
import com.wh01s17.launcher.feature.bar.R

/** El panel del pomodoro, como el de `pomodoro.sh menu`: fase, tiempo, sistema y sus acciones. */
@Composable
internal fun PomodoroPanel(
    pomodoro: PomodoroRepository,
    state: PomodoroState,
    remaining: Long,
    onDismiss: () -> Unit,
) {
    BarPopup(onDismiss) {
        TimerSurface {
            val color = phaseColor(state.phase)
            WhText(stringResource(phaseLabel(state.phase)), style = WhTheme.typography.title, color = color)
            WhText(PomodoroBarModel.clock(remaining), style = WhTheme.typography.display, color = color)
            WhText(
                stringResource(
                    R.string.bar_pomodoro_session,
                    stringResource(presetLabel(state.preset)),
                    state.sessions + 1,
                    state.preset.sessionsPerLongBreak,
                ),
                color = WhTheme.colors.darkForeground,
            )
            Buttons {
                Key(
                    if (state.running) R.string.bar_timer_pause else R.string.bar_timer_start,
                    selected = state.running,
                ) { pomodoro.toggle() }
                Key(R.string.bar_pomodoro_skip) { pomodoro.skip() }
                Key(R.string.bar_pomodoro_reset) { pomodoro.reset() }
                Key(R.string.bar_pomodoro_minus) { pomodoro.adjust(-1) }
                Key(R.string.bar_pomodoro_plus) { pomodoro.adjust(1) }
            }
            WhText(stringResource(R.string.bar_pomodoro_presets), color = WhTheme.colors.darkForeground)
            Buttons {
                PomodoroPreset.entries.forEach { preset ->
                    Key(presetLabel(preset), selected = preset == state.preset) { pomodoro.setPreset(preset) }
                }
            }
        }
    }
}

/** El panel del metrónomo, como el del escritorio: tempo, tap, compás, acento y subdivisión. */
@Composable
internal fun MetronomePanel(
    metronome: MetronomeRepository,
    state: MetronomeState,
    onDismiss: () -> Unit,
) {
    val settings = state.settings
    BarPopup(onDismiss) {
        TimerSurface {
            WhText(stringResource(R.string.bar_metronome_title), style = WhTheme.typography.title, color = WhTheme.colors.accent)
            Row(horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap), verticalAlignment = Alignment.Bottom) {
                WhNumberField(
                    settings.bpm,
                    onValue = { bpm -> metronome.edit { it.copy(bpm = bpm) } },
                    editLabel = stringResource(R.string.bar_metronome_edit_bpm),
                )
                WhText(stringResource(R.string.bar_metronome_bpm, MetronomeMath.tempoName(settings.bpm)))
            }
            WhText(settings.signature, color = WhTheme.colors.darkForeground)
            Buttons {
                TEMPO_STEPS.forEach { step -> Key(if (step > 0) "+$step" else "$step") { metronome.edit { it.copy(bpm = it.bpm + step) } } }
            }
            Buttons {
                Key(
                    if (state.running) R.string.bar_metronome_stop else R.string.bar_timer_start,
                    selected = state.running,
                ) { metronome.toggle() }
                Key(R.string.bar_metronome_tap) { metronome.tap() }
                Key("−") { metronome.edit { it.copy(beats = it.beats - 1) } }
                WhText(stringResource(R.string.bar_metronome_beats, settings.beats), Modifier.align(Alignment.CenterVertically))
                Key("+") { metronome.edit { it.copy(beats = it.beats + 1) } }
                Key(R.string.bar_metronome_accent, selected = settings.accent) { metronome.edit { it.copy(accent = !it.accent) } }
                Key(settings.subdivision.label) { metronome.edit { it.copy(subdivision = it.subdivision.next()) } }
            }
        }
    }
}

private val TEMPO_STEPS = listOf(-5, -1, 1, 5)

private fun Subdivision.next(): Subdivision = Subdivision.entries[(ordinal + 1) % Subdivision.entries.size]

@Composable
private fun TimerSurface(content: @Composable () -> Unit) {
    WhPanel(Modifier.widthIn(max = WhTheme.spacing.menuWidth), contentPadding = WhTheme.spacing.controlGap) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Buttons(content: @Composable FlowRowScope.() -> Unit) {
    val gap = WhTheme.spacing.controlGap
    FlowRow(horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) { content() }
}

@Composable
private fun Key(
    @StringRes label: Int,
    selected: Boolean = false,
    onClick: () -> Unit,
) = Key(stringResource(label), selected, onClick)

@Composable
private fun Key(
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    WhButton(onClick = onClick, selected = selected) { WhText(label, maxLines = 1) }
}

@Composable
internal fun phaseColor(phase: PomodoroPhase) =
    when (phase) {
        PomodoroPhase.Work -> WhTheme.colors.red
        PomodoroPhase.ShortBreak -> WhTheme.colors.accent
        PomodoroPhase.LongBreak -> WhTheme.colors.cyan
    }

@StringRes
private fun phaseLabel(phase: PomodoroPhase): Int =
    when (phase) {
        PomodoroPhase.Work -> R.string.bar_pomodoro_work
        PomodoroPhase.ShortBreak -> R.string.bar_pomodoro_short
        PomodoroPhase.LongBreak -> R.string.bar_pomodoro_long
    }

@StringRes
private fun presetLabel(preset: PomodoroPreset): Int =
    when (preset) {
        PomodoroPreset.Balanced -> R.string.bar_pomodoro_balanced
        PomodoroPreset.Classic -> R.string.bar_pomodoro_classic
        PomodoroPreset.Deep -> R.string.bar_pomodoro_deep
        PomodoroPreset.Ultradian -> R.string.bar_pomodoro_ultradian
    }
