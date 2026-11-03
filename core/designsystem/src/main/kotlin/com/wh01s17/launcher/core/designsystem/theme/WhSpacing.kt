package com.wh01s17.launcher.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wh01s17.launcher.core.designsystem.tokens.WhTokens

/**
 * Medidas de `shell.spacing.toml` y `shell.bar.toml` (plan.md §4.5).
 *
 * Se multiplican por `spacing.scale` y, si `scale-with-font` está activo (lo está), también por
 * el factor de fuente: así el diseño crece junto al texto, como en el shell.
 */
@Immutable
data class WhSpacing(
    val controlGap: Dp,
    val controlPaddingX: Dp,
    val controlPaddingY: Dp,
    /** Fila de popups y listas compactas (`popup-row-height`). */
    val rowHeight: Dp,
    /** Fila del menú wh01s17: el shell la fija en 50 px (`Menu.qml`, `baseRowHeight`). */
    val menuRowHeight: Dp,
    /** Ancho del menú wh01s17 (`Menu.qml`, `cardWidth`). */
    val menuWidth: Dp,
    val rowPaddingX: Dp,
    val panelGap: Dp,
    val panelPadding: Dp,
    val popupPadding: Dp,
    val barHeight: Dp,
    /** Hueco entre tiles (`gaps_in` de `looknfeel.lua`, no sincronizado). */
    val gapsIn: Dp,
    /** Hueco al borde de la pantalla (`gaps_out` de `looknfeel.lua`, no sincronizado). */
    val gapsOut: Dp,
    val borderSize: Dp,
    /** Borde del menú y del launcher: el shell usa `max(1, space(2))` porque su TOML no fija ancho. */
    val menuBorderSize: Dp,
    /** 16 dp = 48 px a 480 dpi = 2 px por píxel del icono (rejilla de 24): nítido. */
    val iconSize: Dp,
    val iconSizeLarge: Dp,
    /** Área táctil mínima: el dibujo es compacto, el toque no (plan.md §7, Hito 11). */
    val touchTarget: Dp,
) {
    companion object {
        /** Medidas que `Menu.qml` escribe en código (en px del shell) en vez de leerlas del TOML. */
        private val SHELL_MENU_ROW_HEIGHT = 50.dp
        private val SHELL_MENU_BORDER = 2.dp
        private val SHELL_MENU_WIDTH = 300.dp

        fun create(fontScale: Float): WhSpacing {
            val s = WhTokens.Spacing
            val factor = s.scale * if (s.scaleWithFont) fontScale else 1f
            return WhSpacing(
                controlGap = s.controlGap * factor,
                controlPaddingX = s.controlPaddingX * factor,
                controlPaddingY = s.controlPaddingY * factor,
                rowHeight = s.popupRowHeight * factor,
                menuRowHeight = SHELL_MENU_ROW_HEIGHT * factor,
                menuWidth = SHELL_MENU_WIDTH * factor,
                rowPaddingX = s.rowPaddingX * factor,
                panelGap = s.panelGap * factor,
                panelPadding = s.panelPadding * factor,
                popupPadding = s.popupPadding * factor,
                barHeight = WhTokens.Bar.sizeVertical * factor,
                gapsIn = 2.dp,
                gapsOut = 2.dp,
                borderSize = WhTokens.Controls.normalBorderWidth,
                menuBorderSize = SHELL_MENU_BORDER * factor,
                iconSize = 16.dp,
                iconSizeLarge = 24.dp,
                touchTarget = 48.dp,
            )
        }
    }
}
