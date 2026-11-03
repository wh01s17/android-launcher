package com.wh01s17.launcher.core.designsystem.modifier

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Extremos de la línea del gradiente para una caja de [size].
 *
 * La dirección es `(cos θ, sin θ)` en coordenadas de pantalla (θ crece hacia abajo): con 45°
 * el color inicial queda arriba a la izquierda y el final abajo a la derecha, como el borde
 * activo de Hyprland (medido en una ventana real). La línea mide lo que la proyección de la
 * caja, de modo que las esquinas opuestas toman justo el primer y el último color.
 */
internal fun gradientLine(
    size: Size,
    angleDegrees: Float,
): Pair<Offset, Offset> {
    val radians = Math.toRadians(angleDegrees.toDouble())
    val dx = cos(radians).toFloat()
    val dy = sin(radians).toFloat()
    val length = abs(size.width * dx) + abs(size.height * dy)
    val center = Offset(size.width / 2f, size.height / 2f)
    val half = Offset(dx * length / 2f, dy * length / 2f)
    return (center - half) to (center + half)
}

/** Gradiente lineal que se adapta al tamaño de la caja en la que se pinta. */
@Immutable
class WhGradientBrush(
    private val colors: List<Color>,
    private val angleDegrees: Float,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val (from, to) = gradientLine(size, angleDegrees)
        return LinearGradientShader(from, to, colors)
    }

    override fun equals(other: Any?): Boolean = other is WhGradientBrush && colors == other.colors && angleDegrees == other.angleDegrees

    override fun hashCode(): Int = 31 * colors.hashCode() + angleDegrees.hashCode()
}

/**
 * Borde del tema (plan.md §4.2): gradiente verde→cian si [active], color plano si no.
 * [alpha] multiplica el alfa propio del color (p. ej. `border-alpha = 0.90` del menú).
 */
@Composable
fun Modifier.whBorder(
    active: Boolean,
    shape: Shape = WhTheme.borders.shape,
    width: Dp = WhTheme.spacing.borderSize,
    alpha: Float = 1f,
): Modifier {
    val borders = WhTheme.borders
    val brush =
        if (active) {
            WhGradientBrush(
                colors = listOf(borders.activeStart.withAlpha(alpha), borders.activeEnd.withAlpha(alpha)),
                angleDegrees = borders.activeAngleDegrees,
            )
        } else {
            SolidColor(borders.inactive.withAlpha(alpha))
        }
    return border(width, brush, shape)
}

private fun Color.withAlpha(factor: Float): Color = copy(alpha = this.alpha * factor)
