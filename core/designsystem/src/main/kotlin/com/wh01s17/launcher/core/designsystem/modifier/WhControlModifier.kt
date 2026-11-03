package com.wh01s17.launcher.core.designsystem.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.wh01s17.launcher.core.designsystem.theme.WhControlState
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

/**
 * Resuelve el estado visual de un control. Prioridad: Pressed > Focus > Selected > Hover > Normal.
 * Un control deshabilitado se queda en Normal (el contenido se atenúa aparte).
 */
@Composable
fun rememberWhControlState(
    interactionSource: InteractionSource,
    selected: Boolean = false,
    enabled: Boolean = true,
): WhControlState {
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    return resolveControlState(enabled, pressed, focused, selected, hovered)
}

internal fun resolveControlState(
    enabled: Boolean,
    pressed: Boolean,
    focused: Boolean,
    selected: Boolean,
    hovered: Boolean,
): WhControlState =
    when {
        !enabled -> WhControlState.Normal
        pressed -> WhControlState.Pressed
        focused -> WhControlState.Focus
        selected -> WhControlState.Selected
        hovered -> WhControlState.Hover
        else -> WhControlState.Normal
    }

/** Relleno y borde de un control según su [state] (`shell.controls.toml`, plan.md §4.3). */
@Composable
fun Modifier.whControlSurface(
    state: WhControlState,
    shape: Shape = WhTheme.borders.shape,
): Modifier {
    val style = WhTheme.controls.style(state)
    return background(style.fill, shape).border(style.borderWidth, style.border, shape)
}

/**
 * Área táctil mínima de 48 dp. El dibujo del control puede ser más compacto: se coloca el
 * dibujo *dentro* de una caja con este modificador, y el toque se recibe en la caja.
 */
@Composable
fun Modifier.whTouchTarget(): Modifier {
    val target = WhTheme.spacing.touchTarget
    return defaultMinSize(minWidth = target, minHeight = target)
}
