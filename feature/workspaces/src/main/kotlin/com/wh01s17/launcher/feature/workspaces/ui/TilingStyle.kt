package com.wh01s17.launcher.feature.workspaces.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wh01s17.launcher.core.config.GeneralConfig
import com.wh01s17.launcher.core.config.WorkspaceAnimation
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.tiling.Gaps

/** Medidas del mosaico: las de `general` de la config o, si no las fija, las del tema (plan.md §4.5). */
@Immutable
internal data class TilingStyle(
    val gapsIn: Dp,
    val gapsOut: Dp,
    val border: Dp,
    val shape: Shape,
)

@Composable
internal fun rememberTilingStyle(general: GeneralConfig): TilingStyle {
    val spacing = WhTheme.spacing
    val borders = WhTheme.borders
    return remember(general, spacing, borders) {
        TilingStyle(
            gapsIn = general.gapsIn?.dp ?: spacing.gapsIn,
            gapsOut = general.gapsOut?.dp ?: spacing.gapsOut,
            border = general.borderSize?.dp ?: spacing.borderSize,
            shape = general.rounding?.let { RoundedCornerShape(it.dp) } ?: borders.shape,
        )
    }
}

/** Huecos para el motor, en px: entre dos tiles hay dos `gaps_in` (Hyprland lo aplica a cada ventana). */
internal fun TilingStyle.gaps(density: Density): Gaps = with(density) { Gaps(inner = 2 * gapsIn.roundToPx(), outer = gapsOut.roundToPx()) }

/** `animations.workspaces` como animación de Compose: la curva de Hyprland durante `speed` décimas. */
internal fun WorkspaceAnimation.spec(): AnimationSpec<Float> =
    if (durationMillis == 0) {
        snap()
    } else {
        tween(durationMillis, easing = CubicBezierEasing(curve.x1, curve.y1, curve.x2, curve.y2))
    }
