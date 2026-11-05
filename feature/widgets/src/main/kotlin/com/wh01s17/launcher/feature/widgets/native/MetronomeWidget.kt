package com.wh01s17.launcher.feature.widgets.native

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.layout.WidgetSize
import com.wh01s17.launcher.core.designsystem.palette.TidalampPalettes
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.timer.ClickVoice
import com.wh01s17.launcher.core.system.timer.MetronomeMath
import com.wh01s17.launcher.core.system.timer.MetronomeRepository
import com.wh01s17.launcher.core.system.timer.MetronomeState
import com.wh01s17.launcher.core.system.timer.Subdivision
import com.wh01s17.launcher.feature.widgets.R

/**
 * Metrónomo (plan.md §7 Hito 7, `wh01s17.metronome` de los dotfiles): tempo y su nombre, compás y
 * subdivisión, un punto por tiempo (el que suena, encendido), ±1 BPM, iniciar/parar, tap tempo,
 * compás y subdivisión al tocar.
 */
@Composable
internal fun MetronomeWidget(
    metronome: MetronomeRepository,
    interactive: Boolean,
    modifier: Modifier = Modifier,
) {
    val state by metronome.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val colors = WhTheme.colors
    BoxWithConstraints(modifier.fillMaxSize().background(colors.background).padding(WhTheme.spacing.controlGap)) {
        // Pequeño: tempo y la banda principal; mediano: además encabezado y compás; grande: todo.
        val size = WidgetSize.of(maxWidth, maxHeight)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
            if (size != WidgetSize.Small) NovaHeading(stringResource(R.string.native_metronome_title), settings.signature)
            if (size >= WidgetSize.Large) Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
                WhText("${settings.bpm}", style = readoutStyle(size), color = colors.accent, maxLines = 1)
                WhText(
                    "BPM · ${MetronomeMath.tempoName(settings.bpm)}",
                    style = WhTheme.typography.bodySmall,
                    color = colors.foreground,
                    maxLines = 1,
                )
            }
            if (size != WidgetSize.Small) BeatDots(state, interactive) { metronome.edit { it.copy(accent = !it.accent) } }
            if (size >= WidgetSize.Large) Legend(settings.accent, settings.subdivision != Subdivision.Quarter)
            Spacer(Modifier.weight(1f))
            Keys(metronome, state, interactive, extended = size >= WidgetSize.Large)
        }
    }
}

/** Las dos bandas nova de teclas: tempo e iniciar/parar; tap tempo, compás y subdivisión. */
@Composable
private fun Keys(
    metronome: MetronomeRepository,
    state: MetronomeState,
    interactive: Boolean,
    extended: Boolean,
) {
    val palette = TidalampPalettes.default
    val settings = state.settings
    NovaBand(palette) {
        NovaKey(stringResource(R.string.native_metronome_slower), { metronome.edit { it.copy(bpm = it.bpm - 1) } }, palette, interactive) {
            WhText("−", color = it)
        }
        NovaKey(stringResource(R.string.native_metronome_toggle), metronome::toggle, palette, interactive, lit = state.running) {
            TransportGlyph(if (state.running) Transport.Stop else Transport.Play, it)
        }
        NovaKey(stringResource(R.string.native_metronome_faster), { metronome.edit { it.copy(bpm = it.bpm + 1) } }, palette, interactive) {
            WhText("+", color = it)
        }
    }
    if (!extended) return
    NovaBand(palette) {
        NovaKey(stringResource(R.string.native_metronome_tap), metronome::tap, palette, interactive) { WhText("tap", color = it) }
        NovaKey(stringResource(R.string.native_metronome_beats), {
            metronome.edit { it.copy(beats = it.beats % MAX_BEATS + 1) }
        }, palette, interactive) {
            WhText("${settings.beats}/4", color = it)
        }
        NovaKey(
            stringResource(R.string.native_metronome_subdivision),
            { metronome.edit { it.copy(subdivision = next(it.subdivision)) } },
            palette,
            interactive,
        ) { WhText(settings.subdivision.label.lowercase(), color = it, maxLines = 1) }
    }
}

/**
 * Un punto por tiempo del compás, del color del sonido que tendrá; tocarlos alterna entre marcar el
 * primer tiempo y que todos suenen igual.
 */
@Composable
private fun BeatDots(
    state: MetronomeState,
    interactive: Boolean,
    onToggleAccent: () -> Unit,
) {
    val description =
        stringResource(if (state.settings.accent) R.string.native_metronome_accent_on else R.string.native_metronome_accent_off)
    Row(
        Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description }
            .clickable(remember { MutableInteractionSource() }, null, enabled = interactive, onClick = onToggleAccent)
            .padding(vertical = WhTheme.spacing.controlPaddingY),
        horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (beat in 0 until state.settings.beats) {
            BeatDot(MetronomeMath.voices(state.settings, beat).first(), sounding = state.running && beat == state.beat)
        }
    }
}

/** Qué sonido es cada color: acento (si se marca el 1), tiempo y, con subdivisión, el pulso. */
@Composable
private fun Legend(
    accent: Boolean,
    subdivided: Boolean,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.panelGap), verticalAlignment = Alignment.CenterVertically) {
        if (accent) LegendItem(ClickVoice.Accent, stringResource(R.string.native_metronome_voice_accent))
        LegendItem(ClickVoice.Beat, stringResource(R.string.native_metronome_voice_beat))
        if (subdivided) LegendItem(ClickVoice.Sub, stringResource(R.string.native_metronome_voice_sub))
    }
}

@Composable
private fun LegendItem(
    voice: ClickVoice,
    label: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap / 2), verticalAlignment = Alignment.CenterVertically) {
        BeatDot(voice, sounding = false, small = true)
        WhText("$label ${voice.hz.toInt()} Hz", style = WhTheme.typography.caption, color = WhTheme.colors.darkForeground, maxLines = 1)
    }
}

private fun next(subdivision: Subdivision) = Subdivision.entries[(subdivision.ordinal + 1) % Subdivision.entries.size]

/** Al pasar de 7/4 vuelve a 1/4: los compases habituales caben en un toque o pocos. */
private const val MAX_BEATS = 7
