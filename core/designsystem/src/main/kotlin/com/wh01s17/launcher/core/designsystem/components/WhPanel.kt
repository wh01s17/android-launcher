package com.wh01s17.launcher.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.wh01s17.launcher.core.designsystem.modifier.whBorder
import com.wh01s17.launcher.core.designsystem.theme.LocalWhContentColor
import com.wh01s17.launcher.core.designsystem.theme.WhSurfaceStyle
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

/**
 * Superficie con marco (menú, popup, tile). [active] pinta el borde con el gradiente verde→cian
 * y, si no, con el color inactivo. El contenido hereda el color de texto de la superficie.
 */
@Composable
fun WhPanel(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    style: WhSurfaceStyle = WhTheme.surfaces.menu.surface,
    contentPadding: Dp = WhTheme.spacing.panelPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = WhTheme.borders.shape
    Column(
        modifier =
            modifier
                .background(style.background, shape)
                .whBorder(active = active, shape = shape, width = style.borderWidth, alpha = style.borderAlpha)
                .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.panelGap),
    ) {
        CompositionLocalProvider(LocalWhContentColor provides style.text) { content() }
    }
}
