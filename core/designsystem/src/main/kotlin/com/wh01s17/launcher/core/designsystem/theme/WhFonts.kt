package com.wh01s17.launcher.core.designsystem.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.wh01s17.launcher.core.designsystem.R

/**
 * Fuentes del tema (plan.md §4.4), sincronizadas desde los dotfiles.
 *
 * Android no aplica la sustitución de glifos de fontconfig que usa el escritorio:
 * los iconos se dibujan siempre con [pixelIcons] (ver `WhIcon`), nunca dentro de un texto.
 */
object WhFonts {
    /** Departure Mono: monoespaciada pixelada, sólo tiene peso Regular. */
    val departureMono = FontFamily(Font(R.font.departure_mono, FontWeight.Normal))

    /** wh01s17 Pixel Icons: Pixelarticons en los code points Nerd Font del shell. */
    val pixelIcons = FontFamily(Font(R.font.wh01s17_pixel_icons, FontWeight.Normal))
}
