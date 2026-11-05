package com.wh01s17.launcher.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.wh01s17.launcher.core.designsystem.tokens.WhTokens

/**
 * Estados de un control (plan.md §4.3). Prioridad al resolverlos:
 * Pressed > Focus > Selected > Hover > Normal (ver `rememberWhControlState`).
 * En táctil no hay *hover*: *pressed* ocupa su lugar.
 */
enum class WhControlState { Normal, Hover, Focus, Selected, Pressed }

/** Aspecto de un control en un estado: relleno y borde ya llevan su alfa aplicado. */
@Immutable
data class WhControlStyle(
    val content: Color,
    val fill: Color,
    val border: Color,
    val borderWidth: Dp,
)

/** Los cinco estilos de `shell.controls.toml`. */
@Immutable
data class WhControls(
    val normal: WhControlStyle,
    val hover: WhControlStyle,
    val focus: WhControlStyle,
    val selected: WhControlStyle,
    val pressed: WhControlStyle,
    /** Fondo de la selección de texto (`selection-fill-alpha`). */
    val textSelection: Color,
) {
    fun style(state: WhControlState): WhControlStyle =
        when (state) {
            WhControlState.Normal -> normal
            WhControlState.Hover -> hover
            WhControlState.Focus -> focus
            WhControlState.Selected -> selected
            WhControlState.Pressed -> pressed
        }

    companion object {
        fun create(remap: (Color) -> Color = { it }): WhControls {
            val c = WhTokens.Controls
            val base =
                WhControls(
                    normal = style(c.normalColor, c.normalFillAlpha, c.normalBorder, c.normalBorderAlpha, c.normalBorderWidth),
                    hover =
                        style(
                            c.hoverCursorColor,
                            c.hoverCursorFillAlpha,
                            c.hoverCursorBorder,
                            c.hoverCursorBorderAlpha,
                            c.hoverCursorBorderWidth,
                        ),
                    focus = style(c.focusColor, c.focusFillAlpha, c.focusBorder, c.focusBorderAlpha, c.focusBorderWidth),
                    selected = style(c.selectedColor, c.selectedFillAlpha, c.selectedBorder, c.selectedBorderAlpha, c.selectedBorderWidth),
                    // El TOML sólo define el relleno de *pressed*: color y borde son los de hover.
                    pressed =
                        style(
                            c.hoverCursorColor,
                            c.pressedFillAlpha,
                            c.hoverCursorBorder,
                            c.hoverCursorBorderAlpha,
                            c.hoverCursorBorderWidth,
                        ),
                    textSelection = c.selectedColor.copy(alpha = c.selectionFillAlpha),
                )
            return base.copy(
                normal = base.normal.remapped(remap),
                hover = base.hover.remapped(remap),
                focus = base.focus.remapped(remap),
                selected = base.selected.remapped(remap),
                pressed = base.pressed.remapped(remap),
                textSelection = remap(base.textSelection),
            )
        }

        private fun style(
            color: Color,
            fillAlpha: Float,
            border: Color,
            borderAlpha: Float,
            borderWidth: Dp,
        ) = WhControlStyle(color, color.copy(alpha = fillAlpha), border.copy(alpha = borderAlpha), borderWidth)

        private fun WhControlStyle.remapped(remap: (Color) -> Color) =
            copy(content = remap(content), fill = remap(fill), border = remap(border))
    }
}
