package com.wh01s17.launcher.feature.widgets.native.hack

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wh01s17.launcher.core.designsystem.components.WhNumberField
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.layout.WidgetSize
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.timer.MetronomeMath
import com.wh01s17.launcher.core.system.timer.MetronomeRepository
import com.wh01s17.launcher.core.system.timer.MetronomeState
import com.wh01s17.launcher.core.system.timer.Subdivision
import com.wh01s17.launcher.feature.widgets.R
import com.wh01s17.launcher.feature.widgets.native.readoutStyle

/**
 * El metrónomo hack: `tick 4/4 1/4` (compás y subdivisión). El tempo (tocarlo lo deja escribir), su nombre, un
 * `[n]` por tiempo con el que suena en vídeo inverso (tocarlos alterna marcar el 1) y las teclas
 * `[-]` `[run]`/`[halt]` `[+]`; en grande, `[tap]`, el compás y la subdivisión.
 */
@Composable
internal fun HackMetronome(
    metronome: MetronomeRepository,
    interactive: Boolean,
    modifier: Modifier,
) {
    val state by metronome.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val colors = WhTheme.colors
    HackFrame(modifier) { size ->
        val subdivision = settings.subdivision.label.lowercase()
        if (size != WidgetSize.Small) HackPrompt("tick ${settings.beats}/4 $subdivision")
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
            WhNumberField(
                settings.bpm,
                onValue = { bpm -> metronome.edit { it.copy(bpm = bpm) } },
                style = readoutStyle(size),
                color = colors.accent,
                enabled = interactive,
                editLabel = stringResource(R.string.native_metronome_edit_bpm),
            )
            WhText("bpm", style = WhTheme.typography.bodySmall, color = colors.darkForeground, maxLines = 1)
        }
        if (size != WidgetSize.Small) {
            HackLine("tempo", MetronomeMath.tempoName(settings.bpm).lowercase())
            HackBeats(state, interactive) { metronome.edit { it.copy(accent = !it.accent) } }
        }
        if (size >= WidgetSize.Large) {
            // El péndulo: de un extremo al otro con cada tiempo, como la varilla de la barra.
            val left = !state.running || state.ticks % 2 == 0L
            HackMeter("swing", if (left) 0f else 1f, "", colors.accent, marker = true)
            HackLine("ticks", "%06d".format(state.ticks))
            HackLine("accent", if (settings.accent) "on" else "off")
        }
        Spacer(Modifier.weight(1f))
        HackKeys {
            HackKey("-", stringResource(R.string.native_metronome_slower), { metronome.edit { it.copy(bpm = it.bpm - 1) } }, interactive)
            HackKey(
                if (state.running) "halt" else "run",
                stringResource(R.string.native_metronome_toggle),
                metronome::toggle,
                interactive,
                lit = state.running,
            )
            HackKey("+", stringResource(R.string.native_metronome_faster), { metronome.edit { it.copy(bpm = it.bpm + 1) } }, interactive)
        }
        if (size >= WidgetSize.Large) {
            HackKeys {
                HackKey("tap", stringResource(R.string.native_metronome_tap), metronome::tap, interactive)
                HackKey(
                    "${settings.beats}/4",
                    stringResource(R.string.native_metronome_beats),
                    { metronome.edit { it.copy(beats = it.beats % MAX_BEATS + 1) } },
                    interactive,
                )
                HackKey(
                    subdivision,
                    stringResource(R.string.native_metronome_subdivision),
                    {
                        metronome.edit {
                            it.copy(
                                subdivision = Subdivision.entries[(it.subdivision.ordinal + 1) % Subdivision.entries.size],
                            )
                        }
                    },
                    interactive,
                )
            }
        }
    }
}

/** `[1][2][3][4]`: el que suena, en vídeo inverso; el 1, en acento si se marca. Tocarlos alterna marcarlo. */
@Composable
private fun HackBeats(
    state: MetronomeState,
    interactive: Boolean,
    onToggleAccent: () -> Unit,
) {
    val colors = WhTheme.colors
    val description =
        stringResource(if (state.settings.accent) R.string.native_metronome_accent_on else R.string.native_metronome_accent_off)
    Row(
        Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description }
            .clickable(remember { MutableInteractionSource() }, null, enabled = interactive, onClick = onToggleAccent)
            .padding(vertical = WhTheme.spacing.controlPaddingY),
        horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap / 2),
    ) {
        for (beat in 0 until state.settings.beats) {
            val sounding = state.running && beat == state.beat
            val accent = beat == 0 && state.settings.accent
            WhText(
                "[${beat + 1}]",
                Modifier.background(if (sounding) colors.accent else colors.background),
                style = WhTheme.typography.bodySmall,
                color =
                    when {
                        sounding -> colors.background
                        accent -> colors.accent
                        else -> colors.foreground
                    },
            )
        }
    }
}

private const val MAX_BEATS = 7
