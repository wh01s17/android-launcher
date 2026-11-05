package com.wh01s17.launcher.core.designsystem.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Cuánto sitio tiene un widget en su tile: el tiling decide el tamaño y el widget se adapta, como
 * una ventana de Hyprland. Pequeño: sólo lo esencial; mediano: sin extras; grande: todo;
 * completo: casi toda la pantalla.
 */
enum class WidgetSize {
    Small,
    Medium,
    Large,

    /** Casi toda la pantalla: sitio para una vista completa (el uso del sistema, al estilo btop). */
    Full,
    ;

    companion object {
        fun of(
            width: Dp,
            height: Dp,
        ): WidgetSize =
            when {
                height < SMALL_HEIGHT || width < SMALL_WIDTH -> Small
                height < LARGE_HEIGHT -> Medium
                height < FULL_HEIGHT -> Large
                else -> Full
            }

        // Medidas de diseño de los widgets propios (no de tema): tiles de 1/4 de pantalla o menos
        // son pequeños; a partir de media pantalla, grandes.
        private val SMALL_HEIGHT = 150.dp
        private val SMALL_WIDTH = 150.dp
        private val LARGE_HEIGHT = 330.dp
        private val FULL_HEIGHT = 560.dp
    }
}
