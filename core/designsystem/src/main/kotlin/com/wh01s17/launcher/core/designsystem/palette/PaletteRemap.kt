package com.wh01s17.launcher.core.designsystem.palette

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import com.wh01s17.launcher.core.designsystem.tokens.WhTokens

/**
 * Lleva los colores del tema wh01s17 (los tokens, que son la paleta `phosphor`) a otra paleta de
 * tidalamp, para que la paleta elegida en Estilo tiña todo el launcher y no sólo el widget de TIDAL.
 *
 * Cada color de los tokens con un papel en la paleta (fondos, superficies, bordes, textos, acento y
 * el cian del segundo tono) se sustituye por su equivalente; se compara el RGB y se conserva el alfa,
 * así que valen también los colores ya con transparencia (bordes, rellenos). Los colores de aviso
 * (rojo, amarillo, naranja, azul, magenta, marrón) no cambian: su significado es el mismo en toda
 * paleta. Con `phosphor` no cambia nada.
 */
class PaletteRemap(
    palette: PlayerPalette,
) {
    private val table: Map<Int, Color>? = if (palette.name == TidalampPalettes.default.name) null else table(palette)

    operator fun invoke(color: Color): Color {
        val mapped = table?.get(rgb(color)) ?: return color
        return mapped.copy(alpha = color.alpha)
    }

    companion object {
        val Identity = PaletteRemap(TidalampPalettes.default)

        /** El segundo tono (el cian de wh01s17): el acento aclarado hacia el texto. */
        fun secondary(palette: PlayerPalette): Color = lerp(palette.accent, palette.foreground, SECONDARY_MIX)

        private fun table(p: PlayerPalette): Map<Int, Color> {
            val c = WhTokens.Colors
            val secondary = secondary(p)
            val surface = lerp(p.background, p.band, HALF)
            return mapOf(
                rgb(c.background) to p.background,
                rgb(c.darkBackground) to lerp(p.background, Color.Black, DARK),
                rgb(c.darkerBackground) to lerp(p.background, Color.Black, DARKER),
                rgb(WhTokens.Bar.background) to surface,
                rgb(c.lighterBackground) to p.band,
                rgb(WhTokens.Controls.normalBorder) to lerp(p.band, p.muted, HALF),
                rgb(c.muted) to p.muted,
                rgb(c.foreground) to p.foreground,
                rgb(c.darkForeground) to p.dim,
                rgb(c.lightForeground) to lerp(p.dim, p.foreground, HALF),
                rgb(c.brightForeground) to lerp(p.foreground, Color.White, HALF),
                rgb(c.accent) to p.accent,
                rgb(c.brightGreen) to lerp(p.accent, Color.White, BRIGHT),
                rgb(c.selectionBackground) to lerp(p.background, p.accent, SELECTION),
                rgb(c.cyan) to secondary,
                rgb(c.brightCyan) to lerp(secondary, Color.White, BRIGHT),
            )
        }

        private fun rgb(color: Color): Int = color.copy(alpha = 1f).toArgb()

        private const val HALF = 0.5f
        private const val DARK = 0.3f
        private const val DARKER = 0.6f
        private const val BRIGHT = 0.3f
        private const val SELECTION = 0.25f
        private const val SECONDARY_MIX = 0.45f
    }
}
