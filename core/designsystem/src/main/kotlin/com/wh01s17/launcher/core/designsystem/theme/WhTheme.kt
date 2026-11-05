package com.wh01s17.launcher.core.designsystem.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Valores por defecto del tema que no vienen de los dotfiles. */
object WhDefaults {
    /** Hardware invariant for OLED wallpapers and dreams: fully off pixels, independent of theme surfaces. */
    val oledBackground = Color.Black

    /** Factor px de escritorio → sp del teléfono (plan.md §4.4). Se ajustará en Ajustes. */
    const val FONT_SCALE = 1.25f
}

private fun missing(name: String): Nothing = error("$name: falta WhTheme { } en la composición")

internal val LocalWhColors = staticCompositionLocalOf<WhColors> { missing("WhColors") }
internal val LocalWhTypography = staticCompositionLocalOf<WhTypography> { missing("WhTypography") }
internal val LocalWhSpacing = staticCompositionLocalOf<WhSpacing> { missing("WhSpacing") }
internal val LocalWhControls = staticCompositionLocalOf<WhControls> { missing("WhControls") }
internal val LocalWhSurfaces = staticCompositionLocalOf<WhSurfaces> { missing("WhSurfaces") }
internal val LocalWhBorders = staticCompositionLocalOf<WhBorders> { missing("WhBorders") }

/** Color por defecto de texto e iconos; cada superficie lo cambia para su contenido. */
val LocalWhContentColor = staticCompositionLocalOf<Color> { missing("LocalWhContentColor") }

/** Acceso a los valores del tema desde cualquier `@Composable`. */
object WhTheme {
    val colors: WhColors
        @Composable @ReadOnlyComposable
        get() = LocalWhColors.current
    val typography: WhTypography
        @Composable @ReadOnlyComposable
        get() = LocalWhTypography.current
    val spacing: WhSpacing
        @Composable @ReadOnlyComposable
        get() = LocalWhSpacing.current
    val controls: WhControls
        @Composable @ReadOnlyComposable
        get() = LocalWhControls.current
    val surfaces: WhSurfaces
        @Composable @ReadOnlyComposable
        get() = LocalWhSurfaces.current
    val borders: WhBorders
        @Composable @ReadOnlyComposable
        get() = LocalWhBorders.current
}

/**
 * Tema `wh01s17`: los valores salen de los tokens generados desde los dotfiles
 * (plan.md §4.1), nunca de literales.
 */
@Composable
fun WhTheme(
    fontScale: Float = WhDefaults.FONT_SCALE,
    content: @Composable () -> Unit,
) {
    val colors = remember { WhColors() }
    val typography = remember(fontScale) { WhTypography.create(fontScale) }
    val spacing = remember(fontScale) { WhSpacing.create(fontScale) }
    val controls = remember { WhControls.create() }
    val surfaces = remember(spacing) { WhSurfaces.create(spacing) }
    val borders = remember { WhBorders() }
    val selection =
        remember(colors, controls) { TextSelectionColors(handleColor = colors.accent, backgroundColor = controls.textSelection) }

    CompositionLocalProvider(
        LocalWhColors provides colors,
        LocalWhTypography provides typography,
        LocalWhSpacing provides spacing,
        LocalWhControls provides controls,
        LocalWhSurfaces provides surfaces,
        LocalWhBorders provides borders,
        LocalWhContentColor provides colors.foreground,
        LocalTextSelectionColors provides selection,
        content = content,
    )
}
