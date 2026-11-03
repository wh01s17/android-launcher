package com.wh01s17.launcher.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.wh01s17.launcher.core.designsystem.tokens.WhTokens

/**
 * Bordes de Hyprland (plan.md §4.2): el activo es un gradiente `activeStart → activeEnd`
 * a [activeAngleDegrees]; el inactivo, un color plano. [shape] es el radio de 10 px del tema.
 */
@Immutable
data class WhBorders(
    val activeStart: Color = WhTokens.Border.activeStart,
    val activeEnd: Color = WhTokens.Border.activeEnd,
    val activeAngleDegrees: Float = WhTokens.Border.activeAngleDegrees,
    val inactive: Color = WhTokens.Border.inactive,
    val shape: Shape = RoundedCornerShape(WhTokens.Border.rounding),
)
