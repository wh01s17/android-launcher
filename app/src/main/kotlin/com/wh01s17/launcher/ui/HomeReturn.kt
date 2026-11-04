package com.wh01s17.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.wh01s17.launcher.core.config.WorkspaceAnimation

/**
 * Animación de vuelta al launcher (plan.md §7 Hito 6): al volver desde una app, el escritorio crece
 * desde [START_SCALE] y aparece, con la curva y la duración de `animations.workspaces` (sin
 * animaciones, no hace nada). En HyperOS con un launcher ajeno el sistema no anima la vuelta.
 *
 * La actividad sólo cuenta las vueltas ([play]); la animación corre en la composición
 * ([modifier]), que es quien tiene el reloj de fotogramas.
 */
class HomeReturn {
    private var returns by mutableIntStateOf(0)

    fun play() {
        returns++
    }

    @Composable
    fun modifier(animation: WorkspaceAnimation?): Modifier {
        val progress = remember { Animatable(1f) }
        LaunchedEffect(returns) {
            val duration = animation?.durationMillis ?: 0
            if (returns == 0 || duration == 0) return@LaunchedEffect
            val curve = animation!!.curve
            progress.snapTo(0f)
            progress.animateTo(1f, tween(duration, easing = CubicBezierEasing(curve.x1, curve.y1, curve.x2, curve.y2)))
        }
        return Modifier.graphicsLayer {
            val t = progress.value
            val scale = START_SCALE + (1f - START_SCALE) * t
            scaleX = scale
            scaleY = scale
            alpha = START_ALPHA + (1f - START_ALPHA) * t
        }
    }

    private companion object {
        const val START_SCALE = 0.94f
        const val START_ALPHA = 0.4f
    }
}
