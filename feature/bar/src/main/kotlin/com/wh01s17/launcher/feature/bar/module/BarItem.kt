package com.wh01s17.launcher.feature.bar.module

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.wh01s17.launcher.core.config.BarModuleConfig
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.modifier.whControlSurface
import com.wh01s17.launcher.core.designsystem.theme.LocalWhContentColor
import com.wh01s17.launcher.core.designsystem.theme.WhControlState
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.bar.LocalBarCompact
import com.wh01s17.launcher.feature.bar.LocalBarSlot

/** Opacidad de un módulo atenuado (`dimmed` de `WidgetButton.qml`). */
private const val DIMMED_ALPHA = 0.45f

/**
 * Los valores de partida de un módulo con los colores del tema (R6). Los módulos pasan aquí sus
 * `classColors` y `dimClasses` propios; la config del usuario los sustituye.
 */
@Composable
internal fun moduleDefaults(
    classColors: Map<String, Color> = emptyMap(),
    dimClasses: Set<String> = emptySet(),
    color: Color = WhTheme.colors.foreground,
): ModuleDefaults = ModuleDefaults(color, WhTheme.colors.muted, classColors, dimClasses)

@Composable
internal fun rememberModuleStyle(
    settings: BarModuleConfig,
    classes: List<String>,
    defaults: ModuleDefaults,
): ModuleStyle = remember(settings, classes, defaults) { ModuleStyleResolver.resolve(settings, classes, defaults) }

/**
 * El equivalente de `WidgetButton`/`StatusModule` del shell: un módulo de la barra con el color de
 * sus clases, atenuado si toca y entre corchetes si se pide. Con [onClick] o [onLongClick] recibe
 * toques y mide el alto del área táctil (48 dp); sin ellos es sólo un indicador.
 *
 * [contentDescription] es lo que lee TalkBack: sin él, sería sólo un glifo suelto. [collapsible]: el
 * módulo está en reposo y la barra puede ocultarlo si no cabe todo. En modo compacto el relleno se reduce.
 */
@Composable
internal fun BarItem(
    style: ModuleStyle,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    collapsible: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val spacing = WhTheme.spacing
    val slot = LocalBarSlot.current
    SideEffect { slot?.collapsible = collapsible }
    val paddingX = if (LocalBarCompact.current) spacing.barItemPaddingXCompact else spacing.barItemPaddingX
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val touch =
        if (onClick != null || onLongClick != null) {
            Modifier.combinedClickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onLongClick = onLongClick,
                onClick = onClick ?: {},
            )
        } else {
            Modifier
        }
    Box(
        modifier =
            modifier
                .height(spacing.touchTarget)
                .alpha(if (style.dimmed) DIMMED_ALPHA else 1f)
                .then(touch)
                .semantics(mergeDescendants = true) { contentDescription?.let { this.contentDescription = it } },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier =
                Modifier
                    .then(if (pressed) Modifier.whControlSurface(WhControlState.Pressed) else Modifier)
                    .padding(horizontal = paddingX, vertical = spacing.controlPaddingY),
            horizontalArrangement = Arrangement.spacedBy(spacing.controlGap / 2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (style.brackets) WhText("[", color = style.bracketColor)
            CompositionLocalProvider(LocalWhContentColor provides style.color) { content() }
            if (style.brackets) WhText("]", color = style.bracketColor)
        }
    }
}
