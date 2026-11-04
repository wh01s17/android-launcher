package com.wh01s17.launcher.feature.widgets.native

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.timer.ClickVoice

/**
 * Una tecla de transporte cuadrada, como `[ x ▮▮ ]` de tidalamp: borde fino, sin redondeo; pulsada
 * o [lit] (encendida), en el verde de acento. Sin [enabled] no responde (modo edición).
 */
@Composable
internal fun RetroKey(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    lit: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = WhTheme.colors
    val spacing = WhTheme.spacing
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val on = pressed || lit
    Row(
        modifier
            .border(spacing.borderSize, if (on) colors.accent else colors.border, RectangleShape)
            .background(if (on) colors.selectionBackground else colors.lighterBackground)
            .semantics { contentDescription = description }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.controlPaddingX, vertical = spacing.controlPaddingY),
        horizontalArrangement = Arrangement.spacedBy(spacing.controlGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * Barra de progreso de tidalamp: una línea en acento hasta la posición, con un punto, y el resto
 * en `border`. Con [onSeek], tocarla salta a esa fracción.
 */
@Composable
internal fun RetroProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    onSeek: ((Float) -> Unit)? = null,
) {
    val colors = WhTheme.colors
    val line = WhTheme.spacing.borderSize
    val dot = WhTheme.spacing.controlGap
    val seek = if (onSeek != null) Modifier.pointerInput(onSeek) { detectTapGestures { onSeek(it.x / size.width) } } else Modifier
    Box(
        modifier.fillMaxWidth().height(WhTheme.spacing.touchTarget / 2).then(seek).drawBehind {
            val y = size.height / 2
            val x = size.width * fraction.coerceIn(0f, 1f)
            val stroke = line.toPx()
            drawLine(colors.border, Offset(x, y), Offset(size.width, y), stroke)
            drawLine(colors.accent, Offset(0f, y), Offset(x, y), stroke)
            drawCircle(colors.accent, dot.toPx() / 2, Offset(x, y))
        },
    )
}

/** Medidor de bloques, como los de tidalamp (`VOL`, `BAL`): relleno en [color] sobre `border`. */
@Composable
internal fun RetroMeter(
    label: String,
    fraction: Float,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = WhTheme.colors.accent,
) {
    val colors = WhTheme.colors
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WhText(label, style = WhTheme.typography.caption, color = colors.darkForeground, maxLines = 1)
        Box(
            Modifier.weight(1f).height(WhTheme.spacing.controlGap * 2).drawBehind {
                drawRect(colors.border)
                drawRect(color, size = size.copy(width = size.width * fraction.coerceIn(0f, 1f)))
            },
        )
        WhText(value, style = WhTheme.typography.caption, color = colors.foreground, maxLines = 1)
    }
}

/** El cuadrado de «parar» (no hay glifo de stop en los iconos pixel). */
@Composable
internal fun StopGlyph(enabled: Boolean = true) {
    val color = if (enabled) WhTheme.colors.foreground else WhTheme.colors.darkForeground
    Box(Modifier.size(WhTheme.spacing.iconSize * STOP_FRACTION).background(color, RectangleShape))
}

/**
 * Un punto del compás: siempre del color del sonido que tendrá ese tiempo (acento en verde, tiempo
 * normal en cian, como los clics de `metronome-engine.py`); el que suena ahora, más grande.
 */
@Composable
internal fun BeatDot(
    voice: ClickVoice,
    sounding: Boolean,
    small: Boolean = false,
) {
    val size =
        WhTheme.spacing.controlGap *
            if (sounding) {
                SOUNDING_DOT
            } else if (small) {
                1
            } else {
                2
            }
    Box(Modifier.size(size).background(voiceColor(voice), CircleShape))
}

@Composable
internal fun voiceColor(voice: ClickVoice) =
    when (voice) {
        ClickVoice.Accent -> WhTheme.colors.accent
        ClickVoice.Beat -> WhTheme.colors.cyan
        ClickVoice.Sub -> WhTheme.colors.darkForeground
    }

private const val SOUNDING_DOT = 3

/** En las teclas retro el cuadrado de parar se ve junto a ▶ y a «−»/«+»: tres cuartos de la caja. */
private const val STOP_FRACTION = 0.75f
