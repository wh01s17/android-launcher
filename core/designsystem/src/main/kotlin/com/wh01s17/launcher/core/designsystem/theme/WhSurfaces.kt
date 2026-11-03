package com.wh01s17.launcher.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wh01s17.launcher.core.designsystem.tokens.WhTokens

/**
 * Superficie con marco: fondo opaco al [WhSurfaceStyle.background] (alfa aplicado) y borde
 * activo (gradiente) con [borderAlpha], como en `shell.menu.toml` o `shell.popups.toml`.
 */
@Immutable
data class WhSurfaceStyle(
    val background: Color,
    val text: Color,
    val borderAlpha: Float,
    val borderWidth: Dp,
)

/**
 * Menú y launcher: superficie, scrim y fila seleccionada (plan.md §4.6).
 *
 * [selectedBorderWidth] es 0 como en el shell: `selected-border` sólo se dibuja si el tema fija
 * `selected-border-width`, y el tema wh01s17 no lo hace. La fila elegida se marca con el fondo y el
 * texto verdes.
 */
@Immutable
data class WhMenuStyle(
    val surface: WhSurfaceStyle,
    val scrim: Color,
    val selectedBackground: Color,
    val selectedText: Color,
    val selectedBorder: Color,
    val selectedBorderWidth: Dp = 0.dp,
)

@Immutable
data class WhBarStyle(
    val surface: WhSurfaceStyle,
    /** Color del workspace activo en la barra (`bar.active`, `#ff6b66`). */
    val active: Color,
    val height: Dp,
)

@Immutable
data class WhNotificationStyle(
    val surface: WhSurfaceStyle,
    /** Barra de cuenta atrás. */
    val countdown: Color,
)

@Immutable
data class WhTooltipStyle(
    val background: Color,
    val text: Color,
    val border: Color,
)

/** Las superficies de los `shell.*.toml` que dibuja el launcher. */
@Immutable
data class WhSurfaces(
    val bar: WhBarStyle,
    val launcher: WhMenuStyle,
    val menu: WhMenuStyle,
    val popup: WhSurfaceStyle,
    val notification: WhNotificationStyle,
    val tooltip: WhTooltipStyle,
) {
    companion object {
        fun create(spacing: WhSpacing): WhSurfaces {
            val bar = WhTokens.Bar
            val launcher = WhTokens.Launcher
            val menu = WhTokens.Menu
            val popups = WhTokens.Popups
            val notifications = WhTokens.Notifications
            val tooltip = WhTokens.Tooltip
            return WhSurfaces(
                bar =
                    WhBarStyle(
                        surface =
                            WhSurfaceStyle(
                                bar.background.copy(alpha = bar.backgroundAlpha),
                                bar.text,
                                bar.borderAlpha,
                                spacing.borderSize,
                            ),
                        active = bar.active,
                        height = spacing.barHeight,
                    ),
                launcher =
                    WhMenuStyle(
                        surface =
                            WhSurfaceStyle(
                                launcher.background.copy(alpha = launcher.backgroundAlpha),
                                launcher.text,
                                launcher.borderAlpha,
                                spacing.menuBorderSize,
                            ),
                        scrim = launcher.scrim.copy(alpha = launcher.scrimAlpha),
                        selectedBackground = launcher.selectedBackground.copy(alpha = launcher.selectedBackgroundAlpha),
                        selectedText = launcher.selectedText,
                        selectedBorder = launcher.selectedBorder.copy(alpha = launcher.selectedBorderAlpha),
                    ),
                menu =
                    WhMenuStyle(
                        surface =
                            WhSurfaceStyle(
                                menu.background.copy(alpha = menu.backgroundAlpha),
                                menu.text,
                                menu.borderAlpha,
                                spacing.menuBorderSize,
                            ),
                        scrim = menu.scrim.copy(alpha = menu.scrimAlpha),
                        selectedBackground = menu.selectedBackground.copy(alpha = menu.selectedBackgroundAlpha),
                        selectedText = menu.selectedText,
                        selectedBorder = menu.selectedBorder.copy(alpha = menu.selectedBorderAlpha),
                    ),
                popup =
                    WhSurfaceStyle(
                        popups.background.copy(alpha = popups.backgroundAlpha),
                        popups.text,
                        popups.borderAlpha,
                        popups.borderWidth,
                    ),
                notification =
                    WhNotificationStyle(
                        surface =
                            WhSurfaceStyle(
                                notifications.background.copy(alpha = notifications.backgroundAlpha),
                                notifications.text,
                                notifications.borderAlpha,
                                notifications.borderWidth,
                            ),
                        countdown = notifications.countdown,
                    ),
                tooltip =
                    WhTooltipStyle(
                        background = tooltip.background.copy(alpha = tooltip.backgroundAlpha),
                        text = tooltip.text,
                        border = tooltip.border.copy(alpha = tooltip.borderAlpha),
                    ),
            )
        }
    }
}
