package com.wh01s17.launcher.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import com.wh01s17.launcher.core.designsystem.icons.WhGlyph
import com.wh01s17.launcher.core.designsystem.modifier.rememberWhControlState
import com.wh01s17.launcher.core.designsystem.modifier.whTouchTarget
import com.wh01s17.launcher.core.designsystem.theme.WhControlState
import com.wh01s17.launcher.core.designsystem.theme.WhMenuStyle
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

private const val DISABLED_ALPHA = 0.4f

/**
 * Fila de un menú o lista: icono opcional, título y texto a la derecha (p. ej. un atajo).
 *
 * Estados (`selected-*` del menú, plan.md §4.6): la fila [selected] o pulsada lleva fondo,
 * borde y texto verdes; con el foco del teclado, el borde cian de los controles.
 * El dibujo mide [com.wh01s17.launcher.core.designsystem.theme.WhSpacing.rowHeight] y el toque 48 dp.
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
    menu: WhMenuStyle = WhTheme.surfaces.menu,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val state = rememberWhControlState(interactionSource, selected, enabled)
    val highlighted = state == WhControlState.Pressed || selected
    val spacing = WhTheme.spacing
    val shape = WhTheme.borders.shape
    val focus = WhTheme.controls.focus
    val background = if (highlighted) menu.selectedBackground else Color.Transparent
    val borderColor =
        when {
            state == WhControlState.Focus -> focus.border
            highlighted -> menu.selectedBorder
            else -> Color.Transparent
        }
    val baseColor = if (highlighted) menu.selectedText else menu.surface.text
    val contentColor = if (enabled) baseColor else baseColor.copy(alpha = DISABLED_ALPHA)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .whTouchTarget()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = spacing.rowHeight)
                    .background(background, shape)
                    .border(spacing.borderSize, borderColor, shape)
                    .padding(horizontal = spacing.rowPaddingX),
            horizontalArrangement = Arrangement.spacedBy(spacing.controlGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) WhIcon(leadingIcon, tint = contentColor)
            WhText(title, Modifier.weight(1f), color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (trailingText != null) {
                WhText(trailingText, style = WhTheme.typography.bodySmall, color = WhTheme.colors.darkForeground, maxLines = 1)
            }
        }
    }
}
