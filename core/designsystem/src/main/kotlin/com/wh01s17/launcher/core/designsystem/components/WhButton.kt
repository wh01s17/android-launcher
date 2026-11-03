package com.wh01s17.launcher.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.wh01s17.launcher.core.designsystem.modifier.rememberWhControlState
import com.wh01s17.launcher.core.designsystem.modifier.whControlSurface
import com.wh01s17.launcher.core.designsystem.modifier.whTouchTarget
import com.wh01s17.launcher.core.designsystem.theme.LocalWhContentColor
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

private const val DISABLED_ALPHA = 0.4f

/**
 * Botón con los estados de `shell.controls.toml`. El dibujo es compacto y el área táctil
 * es de 48 dp ([whTouchTarget]). [selected] lo muestra activado (p. ej. pestaña elegida).
 */
@Composable
fun WhButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit,
) {
    val state = rememberWhControlState(interactionSource, selected, enabled)
    val style = WhTheme.controls.style(state)
    val spacing = WhTheme.spacing
    val contentColor = if (enabled) style.content else style.content.copy(alpha = DISABLED_ALPHA)
    Box(
        modifier =
            modifier
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
                    .whControlSurface(state)
                    .padding(horizontal = spacing.controlPaddingX, vertical = spacing.controlPaddingY),
            horizontalArrangement = Arrangement.spacedBy(spacing.controlGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalWhContentColor provides contentColor) { content() }
        }
    }
}
