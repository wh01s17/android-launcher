package com.wh01s17.launcher.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.wh01s17.launcher.core.designsystem.palette.PlayerPalette

/**
 * Contraste WCAG 2.x del texto (plan.md §7 Hito 11: AA, 4,5:1 para texto normal).
 *
 * Los tokens vienen de los dotfiles y no se editan a mano (§4.1); por eso el tema no cambia la paleta,
 * sino que aclara (u oscurece, en un fondo claro) lo justo un color de texto que no llegue al mínimo
 * sobre la superficie más clara donde se escribe. Con phosphor sólo se mueve `darkForeground`
 * (`#748178` → algo más claro: 4,14:1 sobre los tiles). Ver docs/accessibility.md.
 */
object WhContrast {
    /** AA para texto normal; Departure Mono a 10–16 sp nunca es «texto grande». */
    const val AA_TEXT = 4.5f

    private const val STEPS = 64
    private const val LUMINANCE_OFFSET = 0.05f
    private const val DARK_BACKGROUND = 0.5f

    fun ratio(
        foreground: Color,
        background: Color,
    ): Float {
        val bg = background.compositeOver(Color.Black)
        val fg = foreground.compositeOver(bg)
        val a = fg.luminance() + LUMINANCE_OFFSET
        val b = bg.luminance() + LUMINANCE_OFFSET
        return maxOf(a, b) / minOf(a, b)
    }

    /**
     * El color más parecido a [color] que llega a [minimum] sobre todos los [backgrounds]: se mezcla
     * hacia el blanco (fondos oscuros) o el negro en pasos de 1/64 y se queda con el primero que sirve.
     */
    fun ensure(
        color: Color,
        backgrounds: List<Color>,
        minimum: Float = AA_TEXT,
    ): Color {
        if (backgrounds.all { ratio(color, it) >= minimum }) return color
        val darkest = backgrounds.minBy { it.compositeOver(Color.Black).luminance() }
        val target = if (darkest.compositeOver(Color.Black).luminance() < DARK_BACKGROUND) Color.White else Color.Black
        return (1..STEPS)
            .asSequence()
            .map { lerp(color, target, it / STEPS.toFloat()).copy(alpha = color.alpha) }
            .firstOrNull { candidate -> backgrounds.all { ratio(candidate, it) >= minimum } }
            ?: target.copy(alpha = color.alpha)
    }
}

/** Fill de la fila seleccionada del menú sobre su panel (plan.md §4.6): el fondo de texto más claro. */
private const val SELECTED_ROW_ALPHA = 0.10f

/**
 * Los colores de texto con al menos AA sobre las superficies con texto: el fondo, la barra y los
 * paneles ([surface]), los tiles (`lighterBackground`) y la fila seleccionada del menú. Quedan fuera,
 * con motivo, `muted` (corchetes y separadores: decoración) y los rellenos de *pressed* (instantáneos).
 */
fun WhColors.readable(surface: Color): WhColors {
    val backgrounds =
        listOf(
            background,
            surface,
            lighterBackground,
            accent.copy(alpha = SELECTED_ROW_ALPHA).compositeOver(surface),
        )

    fun Color.aa() = WhContrast.ensure(this, backgrounds)
    return copy(
        foreground = foreground.aa(),
        darkForeground = darkForeground.aa(),
        lightForeground = lightForeground.aa(),
        accent = accent.aa(),
        green = green.aa(),
        cyan = cyan.aa(),
        red = red.aa(),
        orange = orange.aa(),
        yellow = yellow.aa(),
        blue = blue.aa(),
        magenta = magenta.aa(),
    )
}

/** Lo mismo para la paleta que pintan los widgets nova (TIDAL…): su texto sobre su fondo y su banda. */
fun PlayerPalette.readable(): PlayerPalette {
    val backgrounds = listOf(background, band)
    return copy(
        foreground = WhContrast.ensure(foreground, backgrounds),
        dim = WhContrast.ensure(dim, backgrounds),
        accent = WhContrast.ensure(accent, backgrounds),
    )
}
