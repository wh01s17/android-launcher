package com.wh01s17.launcher.feature.bar.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wh01s17.launcher.core.designsystem.theme.LocalWhContentColor
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.timer.MetronomeRepository
import com.wh01s17.launcher.feature.bar.R
import com.wh01s17.launcher.feature.bar.module.BarItem
import com.wh01s17.launcher.feature.bar.module.BarModule
import com.wh01s17.launcher.feature.bar.module.BarModuleContext
import com.wh01s17.launcher.feature.bar.module.moduleDefaults
import com.wh01s17.launcher.feature.bar.module.rememberModuleStyle
import kotlin.math.max

/**
 * `wh01s17.metronome`: el metrónomo analógico de la barra del escritorio (`MetronomeIcon.qml`),
 * sobre el mismo [MetronomeRepository] que el tile. En verde mientras suena, con el péndulo
 * cambiando de lado en cada tiempo. Tocar lo inicia o lo para (el clic derecho del escritorio:
 * en el teléfono es lo más usado); mantener pulsado abre el panel con tempo, tap y compás.
 *
 * Los ajustes viven en el repositorio (los comparte el tile), no en la config: las opciones
 * `bpm`, `beats`… de `shell.json` no se leen aquí.
 */
internal class MetronomeModule(
    private val metronome: MetronomeRepository,
) : BarModule {
    override val id = ID

    @Composable
    override fun Content(context: BarModuleContext) {
        val state by metronome.state.collectAsStateWithLifecycle()
        var panelOpen by rememberSaveable { mutableStateOf(false) }
        val defaults = moduleDefaults(classColors = mapOf(RUNNING to WhTheme.colors.accent))
        val style = rememberModuleStyle(context.settings, listOf(if (state.running) RUNNING else STOPPED), defaults)
        val description =
            if (state.running) {
                stringResource(
                    R.string.bar_metronome,
                    state.settings.signature,
                )
            } else {
                stringResource(R.string.bar_metronome_stopped)
            }
        BarItem(
            style = style,
            contentDescription = description,
            collapsible = !state.running,
            onClick = metronome::toggle,
            onLongClick = { panelOpen = true },
        ) {
            MetronomeIcon(running = state.running, ticks = state.ticks, bpm = state.settings.bpm)
        }
        if (panelOpen) MetronomePanel(metronome, state, onDismiss = { panelOpen = false })
    }

    companion object {
        const val ID = "wh01s17.metronome"
        private const val RUNNING = "running"
        private const val STOPPED = "stopped"
    }
}

/**
 * La caja trapezoidal sobre su base, con la varilla y su pesa. Parado, la varilla descansa
 * inclinada (se lee como metrónomo, no como un triángulo); sonando, tarda un tiempo en ir de un
 * lado al otro, como el `Behavior on angle` del escritorio.
 */
@Composable
internal fun MetronomeIcon(
    running: Boolean,
    ticks: Long,
    bpm: Int,
    modifier: Modifier = Modifier,
    height: Dp = WhTheme.spacing.iconSize,
    color: Color = LocalWhContentColor.current,
) {
    val target =
        if (!running) {
            REST_ANGLE
        } else if (ticks % 2 == 0L) {
            -SWING_ANGLE
        } else {
            SWING_ANGLE
        }
    val duration = if (running) (MILLIS_PER_MINUTE / bpm.coerceAtLeast(1)) else REST_MILLIS
    val angle by animateFloatAsState(target, tween(duration), label = "pendulum")
    Canvas(modifier.size(height * WIDTH_RATIO, height)) { drawMetronome(angle, color) }
}

private fun DrawScope.drawMetronome(
    angle: Float,
    color: Color,
) {
    val w = size.width
    val h = size.height
    val line = max(MIN_STROKE_PX, h / STROKE_DIVISOR)
    val half = line / 2
    val stroke = Stroke(width = line, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val cx = w / 2
    val baseY = h * BASE_Y
    val case =
        Path().apply {
            moveTo(cx - w * TOP_HALF, h * TOP_Y + half)
            lineTo(cx + w * TOP_HALF, h * TOP_Y + half)
            lineTo(cx + w * BASE_HALF - half, baseY)
            lineTo(cx - w * BASE_HALF + half, baseY)
            close()
        }
    drawPath(case, color, style = stroke)
    drawRect(color, Offset(half, baseY), Size(w - line, h - half - baseY), style = stroke)
    // La varilla gira sobre un pivote bajo y asoma por encima de la caja.
    val pivot = Offset(cx, h * PIVOT_Y)
    rotate(angle, pivot) {
        val length = h * ROD_LENGTH
        drawLine(color, pivot, Offset(cx, pivot.y - length), strokeWidth = line, cap = StrokeCap.Round)
        val weight = max(line * WEIGHT_STROKES, h * WEIGHT_SIZE)
        val center = Offset(cx, pivot.y - length * WEIGHT_AT)
        drawRect(color, Offset(center.x - weight, center.y - weight * WEIGHT_ASPECT), Size(weight * 2, weight * 2 * WEIGHT_ASPECT))
    }
}

// Proporciones de `MetronomeIcon.qml`, en fracciones del alto o del ancho.
private const val WIDTH_RATIO = 0.86f
private const val TOP_Y = 0.08f
private const val BASE_Y = 0.80f
private const val TOP_HALF = 0.13f
private const val BASE_HALF = 0.40f
private const val PIVOT_Y = 0.70f
private const val ROD_LENGTH = 0.70f
private const val WEIGHT_AT = 0.62f
private const val WEIGHT_SIZE = 0.1f
private const val WEIGHT_STROKES = 1.3f
private const val WEIGHT_ASPECT = 0.8f
private const val STROKE_DIVISOR = 10f
private const val MIN_STROKE_PX = 1.5f
private const val REST_ANGLE = 18f
private const val SWING_ANGLE = 24f
private const val REST_MILLIS = 180
private const val MILLIS_PER_MINUTE = 60_000
