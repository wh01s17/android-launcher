package com.wh01s17.launcher.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

/**
 * Burbuja de tooltip (`shell.tooltip.toml`): fondo `lighter_background`, borde `muted`.
 * Es sólo la superficie; quien la muestre decide dónde y cuándo.
 */
@Composable
fun WhTooltip(
    text: String,
    modifier: Modifier = Modifier,
) {
    val style = WhTheme.surfaces.tooltip
    val spacing = WhTheme.spacing
    val shape = WhTheme.borders.shape
    WhText(
        text = text,
        modifier =
            modifier
                .background(style.background, shape)
                .border(spacing.borderSize, style.border, shape)
                .padding(horizontal = spacing.controlPaddingX, vertical = spacing.controlPaddingY),
        style = WhTheme.typography.bodySmall,
        color = style.text,
    )
}
