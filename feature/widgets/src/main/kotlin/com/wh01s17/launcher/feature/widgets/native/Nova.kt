package com.wh01s17.launcher.feature.widgets.native

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.layout.WidgetSize
import com.wh01s17.launcher.core.designsystem.palette.PlayerPalette
import com.wh01s17.launcher.core.designsystem.palette.TidalampPalettes
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

/*
 * El tema *nova* de tidalamp (`styles/looks.tcss`, `transport.py`): sin marcos ni cajas, un solo
 * fondo, el título con una barra de acento («▍ tidalamp») y el estado sólo en color y en una regla
 * bajo el control que lo tiene. Los colores salen de una [PlayerPalette] de tidalamp.
 */

/** «▍ tidalamp» en acento, a la izquierda; a la derecha, [trailing] apagado (la paleta). */
@Composable
internal fun NovaTitle(
    text: String,
    trailing: String,
    palette: PlayerPalette,
    description: String,
    onClick: (() -> Unit)?,
) {
    val spacing = WhTheme.spacing
    val click =
        if (onClick != null) {
            Modifier.semantics { contentDescription = description }.clickable(
                remember { MutableInteractionSource() },
                null,
                onClick = onClick,
            )
        } else {
            Modifier
        }
    Row(
        Modifier.fillMaxWidth().then(click).padding(vertical = spacing.controlPaddingY),
        horizontalArrangement = Arrangement.spacedBy(spacing.controlGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(spacing.barUnderlineHeight).height(spacing.iconSize).background(palette.accent))
        WhText(text, Modifier.weight(1f), color = palette.accent, maxLines = 1)
        WhText(trailing, style = WhTheme.typography.caption, color = palette.dim, maxLines = 1)
    }
}

/** La línea de avance de tidalamp: fina, en acento hasta un punto, y en `muted` el resto. Tocarla salta. */
@Composable
internal fun NovaProgress(
    fraction: Float,
    palette: PlayerPalette,
    onSeek: ((Float) -> Unit)?,
) {
    val line = WhTheme.spacing.borderSize
    val dot = WhTheme.spacing.controlGap
    val seek = if (onSeek != null) Modifier.pointerInput(onSeek) { detectTapGestures { onSeek(it.x / size.width) } } else Modifier
    Box(
        Modifier.fillMaxWidth().height(WhTheme.spacing.touchTarget / 2).then(seek).drawBehind {
            val y = size.height / 2
            val x = size.width * fraction.coerceIn(0f, 1f)
            val stroke = line.toPx()
            drawLine(palette.muted, Offset(x, y), Offset(size.width, y), stroke)
            drawLine(palette.accent, Offset(0f, y), Offset(x, y), stroke)
            drawCircle(palette.accent, dot.toPx() / 2, Offset(x, y))
        },
    )
}

/** La banda de transporte de nova: el fondo `band` de la paleta y los botones sin caja. */
@Composable
internal fun NovaBand(
    palette: PlayerPalette,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(palette.band).padding(vertical = WhTheme.spacing.controlPaddingY),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * Un botón de nova: sólo el símbolo, sin caja; encendido ([lit]), en acento y con una regla de
 * acento debajo, como shuffle/repeat en tidalamp. Sin [enabled] no responde (modo edición).
 */
@Composable
internal fun NovaKey(
    description: String,
    onClick: () -> Unit,
    palette: PlayerPalette,
    enabled: Boolean,
    lit: Boolean = false,
    content: @Composable (tint: Color) -> Unit,
) {
    val spacing = WhTheme.spacing
    Column(
        Modifier
            .semantics { contentDescription = description }
            .clickable(remember { MutableInteractionSource() }, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.controlPaddingX, vertical = spacing.controlPaddingY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.controlGap / 2),
    ) {
        content(if (lit) palette.accent else palette.foreground)
        Box(Modifier.width(spacing.iconSize).height(spacing.borderSize).background(if (lit) palette.accent else palette.band))
    }
}

/** Los glifos pixel llenan la mitad de su caja: el cuadrado de parar, igual. */
private const val STOP_FRACTION = 0.5f

/**
 * El encabezado nova («▍ pomodoro») con la paleta *phosphor*, que son los colores wh01s17, para
 * los widgets propios; [trailing] va apagado a la derecha.
 */
@Composable
internal fun NovaHeading(
    text: String,
    trailing: String = "",
) = NovaTitle(text, trailing, TidalampPalettes.default, text, onClick = null)

/** La barra de avance fina de nova, en [color] sobre `muted`, sin etiquetas. */
@Composable
internal fun NovaMeter(
    fraction: Float,
    color: Color,
) {
    val muted = TidalampPalettes.default.muted
    Box(
        Modifier.fillMaxWidth().height(WhTheme.spacing.controlGap).drawBehind {
            drawRect(muted)
            drawRect(color, size = size.copy(width = size.width * fraction.coerceIn(0f, 1f)))
        },
    )
}

/** La cifra grande de un widget, más grande cuanto más sitio tiene (grande ×1,5, completo ×2). */
@Composable
internal fun readoutStyle(size: WidgetSize): TextStyle {
    val base = WhTheme.typography.displayLarge
    val scale =
        when (size) {
            WidgetSize.Large -> LARGE_SCALE
            WidgetSize.Full -> FULL_SCALE
            else -> 1f
        }
    return base.copy(fontSize = base.fontSize * scale)
}

private const val LARGE_SCALE = 1.5f
private const val FULL_SCALE = 2f
