package com.wh01s17.launcher.feature.widgets.native.hack

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.layout.WidgetSize
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/*
 * La temática *hack* (`WidgetTheme.Hack`): los widgets como la salida de una terminal. Prompt
 * `~$ orden`, líneas `> clave valor`, barras ASCII `[####....]`, un cursor que parpadea y
 * teclas `[run]` que se encienden en vídeo inverso. Respeta las reglas del tema (plan.md §4.7):
 * colores planos de la paleta, sin brillo, ruido, CRT ni degradados; lo «hacker» es el lenguaje.
 */

private const val BLINK_MILLIS = 530L
private const val MIN_CELLS = 4

/** Corchetes y espacios que rodean la barra en una línea de [HackMeter]. */
private const val METER_FRAME_CHARS = 4

/** El marco de un widget hack: el fondo del tema, su margen y una columna; [content] recibe el tamaño. */
@Composable
internal fun HackFrame(
    modifier: Modifier,
    content: @Composable ColumnScope.(WidgetSize) -> Unit,
) {
    val spacing = WhTheme.spacing
    BoxWithConstraints(modifier.fillMaxSize().background(WhTheme.colors.background).padding(spacing.controlGap)) {
        val size = WidgetSize.of(maxWidth, maxHeight)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.controlGap / 2)) { content(size) }
    }
}

/**
 * `~$ orden`: el prompt en acento y la orden en texto. Corto a propósito (sin `usuario@máquina`): en un
 * tile mediano caben unos 24 caracteres y la orden es lo que cuenta.
 */
@Composable
internal fun HackPrompt(command: String) {
    val colors = WhTheme.colors
    val text =
        buildAnnotatedString {
            withStyle(SpanStyle(color = colors.accent)) { append("~$ ") }
            withStyle(SpanStyle(color = colors.foreground)) { append(command) }
        }
    WhText(text, style = WhTheme.typography.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Una línea de salida: `> clave valor`, con la clave apagada y alineada a [keyWidth] caracteres. */
@Composable
internal fun HackLine(
    key: String,
    value: String,
    color: Color = WhTheme.colors.foreground,
    keyWidth: Int = KEY_WIDTH,
) {
    val dim = WhTheme.colors.darkForeground
    val text =
        buildAnnotatedString {
            withStyle(SpanStyle(color = dim)) { append("> ${key.padEnd(keyWidth)} ") }
            withStyle(SpanStyle(color = color)) { append(value) }
        }
    WhText(text, style = WhTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

private const val KEY_WIDTH = 6

/**
 * El cursor de bloque que parpadea, de la altura de [style]. Sólo se repinta dos veces por segundo; con
 * [lit], el que llama marca el ritmo (el reloj lo enciende y apaga con cada segundo).
 */
@Composable
internal fun HackCursor(
    style: TextStyle,
    modifier: Modifier = Modifier,
    lit: Boolean? = null,
) {
    val blinking by produceState(true, lit) {
        while (lit == null) {
            delay(BLINK_MILLIS)
            value = !value
        }
    }
    val on = lit ?: blinking
    val color = WhTheme.colors.accent
    val height = with(LocalDensity.current) { style.fontSize.toDp() }
    Box(
        modifier
            .padding(start = WhTheme.spacing.controlGap / 2)
            .width(WhTheme.spacing.controlGap)
            .height(height)
            .drawBehind { if (on) drawRect(color) },
    )
}

/**
 * Una barra ASCII que llena el ancho: `label [#######......] suffix`. Cuántas celdas caben se mide con
 * la fuente (monoespaciada), así la línea nunca se parte.
 */
@Composable
internal fun HackMeter(
    label: String,
    fraction: Float,
    suffix: String,
    color: Color,
    modifier: Modifier = Modifier,
    marker: Boolean = false,
) {
    val style = WhTheme.typography.bodySmall
    val colors = WhTheme.colors
    val measurer = rememberTextMeasurer()
    val charPx =
        remember(style, measurer) {
            measurer
                .measure("#", style)
                .size.width
                .coerceAtLeast(1)
        }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = constraints.maxWidth / charPx
        val cells = (columns - label.length - suffix.length - METER_FRAME_CHARS).coerceAtLeast(MIN_CELLS)
        val filled = (fraction.coerceIn(0f, 1f) * cells).roundToInt()
        // Con [marker], una sola `|` en la posición (un péndulo, un cursor) en vez de la barra llena.
        val at = (fraction.coerceIn(0f, 1f) * (cells - 1)).roundToInt()
        val text =
            buildAnnotatedString {
                withStyle(SpanStyle(color = colors.darkForeground)) { append(if (label.isEmpty()) "[" else "$label [") }
                if (marker) {
                    withStyle(SpanStyle(color = colors.muted)) { append(".".repeat(at)) }
                    withStyle(SpanStyle(color = color)) { append("|") }
                    withStyle(SpanStyle(color = colors.muted)) { append(".".repeat(cells - at - 1)) }
                } else {
                    withStyle(SpanStyle(color = color)) { append("#".repeat(filled)) }
                    withStyle(SpanStyle(color = colors.muted)) { append(".".repeat(cells - filled)) }
                }
                withStyle(SpanStyle(color = colors.darkForeground)) { append("] ") }
                withStyle(SpanStyle(color = colors.foreground)) { append(suffix) }
            }
        WhText(text, style = style, maxLines = 1, overflow = TextOverflow.Clip)
    }
}

/**
 * Una tecla `[label]`: en acento y, encendida ([lit]), en vídeo inverso (texto del fondo sobre el
 * acento). [description] es lo que lee TalkBack. Sin [enabled] no responde (modo edición).
 */
@Composable
internal fun HackKey(
    label: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
    lit: Boolean = false,
) {
    val colors = WhTheme.colors
    val spacing = WhTheme.spacing
    WhText(
        "[$label]",
        Modifier
            .semantics { contentDescription = description }
            .clickable(remember { MutableInteractionSource() }, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .background(if (lit) colors.accent else colors.background)
            // Sin relleno lateral para que quepan tres en un tile mediano; el área táctil la amplía
            // Compose hasta 48 dp.
            .padding(vertical = spacing.controlPaddingY),
        style = WhTheme.typography.bodySmall,
        color = if (lit) colors.background else colors.accent,
        maxLines = 1,
    )
}

/** Una fila de teclas repartidas a lo ancho. */
@Composable
internal fun HackKeys(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
