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
    val rowHeight: Dp,
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
    /** 16 dp = 48 px a 480 dpi = 2 px por píxel del icono (rejilla de 24): nítido. */
    val iconSize: Dp,
    val iconSizeLarge: Dp,
    /** Área táctil mínima: el dibujo es compacto, el toque no (plan.md §7, Hito 11). */
    val touchTarget: Dp,
) {
    companion object {
        fun create(fontScale: Float): WhSpacing {
            val s = WhTokens.Spacing
            val factor = s.scale * if (s.scaleWithFont) fontScale else 1f
            return WhSpacing(
                controlGap = s.controlGap * factor,
                controlPaddingX = s.controlPaddingX * factor,
                controlPaddingY = s.controlPaddingY * factor,
                rowHeight = s.popupRowHeight * factor,
                rowPaddingX = s.rowPaddingX * factor,
                panelGap = s.panelGap * factor,
                panelPadding = s.panelPadding * factor,
                popupPadding = s.popupPadding * factor,
                barHeight = WhTokens.Bar.sizeVertical * factor,
                gapsIn = 2.dp,
                gapsOut = 2.dp,
                borderSize = WhTokens.Controls.normalBorderWidth,
                iconSize = 16.dp,
                iconSizeLarge = 24.dp,
                touchTarget = 48.dp,
            )
        }
    }
}
