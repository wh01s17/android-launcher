package com.wh01s17.launcher.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wh01s17.launcher.core.designsystem.icons.WhGlyph
import com.wh01s17.launcher.core.designsystem.modifier.rememberWhControlState
import com.wh01s17.launcher.core.designsystem.modifier.whTouchTarget
import com.wh01s17.launcher.core.designsystem.theme.WhControlState
import com.wh01s17.launcher.core.designsystem.theme.WhMenuStyle
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

private const val DISABLED_ALPHA = 0.4f

/** Opacidad del `›` de los submenús en `Menu.qml`. */
private const val CHEVRON_ALPHA = 0.36f
private const val CHEVRON = "›"

/**
 * Tamaño de la fila. [Popup] es la fila compacta de popups y listas (`popup-row-height`);
 * [Menu] reproduce la del menú wh01s17 (`Menu.qml`): 50 px, texto `heading` e icono grande.
 */
enum class WhListRowSize { Popup, Menu }

/**
 * Fila de un menú o lista: icono opcional, título, texto a la derecha (p. ej. un atajo) y, si
 * [chevron], el `›` que marca un submenú.
 *
 * Estados (`selected-*` del menú, plan.md §4.6): la fila [selected] o pulsada lleva fondo y texto
 * verdes, sin borde, como en el shell ([WhMenuStyle.selectedBorderWidth]); con el foco del
 * teclado, el borde cian de los controles. El toque siempre mide al menos 48 dp.
 */
@Composable
fun WhListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: WhGlyph? = null,
    trailingText: String? = null,
    chevron: Boolean = false,
    size: WhListRowSize = WhListRowSize.Popup,
    menu: WhMenuStyle = WhTheme.surfaces.menu,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val iconSize = rowMetrics(size).iconSize
    WhListRow(
        title = AnnotatedString(title),
        onClick = onClick,
        modifier = modifier,
        selected = selected,
        enabled = enabled,
        leading = leadingIcon?.let { glyph -> { tint -> WhIcon(glyph, tint = tint, size = iconSize) } },
        trailingText = trailingText,
        chevron = chevron,
        size = size,
        menu = menu,
        onLongClick = onLongClick,
        interactionSource = interactionSource,
    )
}

/**
 * Variante con [title] enriquecido (p. ej. las letras que coinciden con una búsqueda) y un
 * [leading] libre, como el icono de una app. [leading] recibe el color del contenido de la fila.
 */
@Composable
fun WhListRow(
    title: AnnotatedString,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    leading: (@Composable (contentColor: Color) -> Unit)? = null,
    trailingText: String? = null,
    chevron: Boolean = false,
    size: WhListRowSize = WhListRowSize.Popup,
    menu: WhMenuStyle = WhTheme.surfaces.menu,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val state = rememberWhControlState(interactionSource, selected, enabled)
    val highlighted = state == WhControlState.Pressed || selected
    val metrics = rowMetrics(size)
    val spacing = WhTheme.spacing
    val shape = WhTheme.borders.shape
    val focused = state == WhControlState.Focus
    val background = if (highlighted) menu.selectedBackground else Color.Transparent
    val (borderWidth, borderColor) =
        when {
            focused -> WhTheme.controls.focus.let { it.borderWidth to it.border }
            highlighted -> menu.selectedBorderWidth to menu.selectedBorder
            else -> 0.dp to Color.Transparent
        }
    val baseColor = if (highlighted) menu.selectedText else menu.surface.text
    val contentColor = if (enabled) baseColor else baseColor.copy(alpha = DISABLED_ALPHA)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .whTouchTarget()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onLongClick = onLongClick,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = metrics.height)
                    .background(background, shape)
                    // `border(0.dp)` pinta una línea de 1 px (hairline): sin ancho, no se aplica.
                    .then(if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, shape) else Modifier)
                    .padding(horizontal = spacing.rowPaddingX),
            horizontalArrangement = Arrangement.spacedBy(spacing.controlGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke(contentColor)
            WhText(
                title,
                Modifier.weight(1f),
                style = metrics.textStyle,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (trailingText != null) {
                WhText(trailingText, style = WhTheme.typography.bodySmall, color = WhTheme.colors.darkForeground, maxLines = 1)
            }
            if (chevron) {
                WhText(CHEVRON, style = metrics.textStyle, color = contentColor.copy(alpha = contentColor.alpha * CHEVRON_ALPHA))
            }
        }
    }
}

private class RowMetrics(
    val height: Dp,
    val textStyle: TextStyle,
    val iconSize: Dp,
)

@Composable
private fun rowMetrics(size: WhListRowSize): RowMetrics {
    val spacing = WhTheme.spacing
    val typography = WhTheme.typography
    return when (size) {
        WhListRowSize.Popup -> RowMetrics(spacing.rowHeight, typography.body, spacing.iconSize)
        WhListRowSize.Menu -> RowMetrics(spacing.menuRowHeight, typography.heading, spacing.iconSizeLarge)
    }
}
