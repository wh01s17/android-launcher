package com.wh01s17.launcher.core.designsystem.screensaver

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.wh01s17.launcher.core.designsystem.theme.WhDefaults
import com.wh01s17.launcher.core.designsystem.tokens.WhTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.sin

/**
 * El salvapantallas: la W de puntos de `w-spin.py` girando sobre negro OLED. Los puntos braille se
 * dibujan con paso cuadrado (el de una celda de terminal de 1 × 2 partida en 2 × 4), sin depender de
 * la fuente. Los fotogramas se calculan fuera del hilo de UI y sólo mientras la vista se ve y está
 * adjunta.
 */
@Suppress("MagicNumber") // Tiempos de cámara y encuadre relativo, independientes de la densidad.
class WhSpinView(
    context: Context,
    accent: Color = WhTokens.Colors.accent,
    /** Si llega, la W sigue sus cambios mientras se ve (la paleta puede cargarse después). */
    private val accents: Flow<Color>? = null,
) : View(context) {
    private var accent = accent
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private var frames: Job? = null
    private var scope: CoroutineScope? = null
    private var levels = IntArray(0)
    private val started = SystemClock.elapsedRealtime()

    /** Coordenadas por nivel de brillo, reutilizadas en cada fotograma para no crear basura. */
    private val points = Array(SpinGeometry.LEVELS + 1) { FloatArray(SpinGeometry.DOT_WIDTH * SpinGeometry.DOT_HEIGHT * 2) }
    private val counts = IntArray(SpinGeometry.LEVELS + 1)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        accents?.let { flow ->
            scope?.launch {
                flow.collect {
                    accent = it
                    invalidate()
                }
            }
        }
        animateFrames()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        frames?.cancel()
        if (visibility == VISIBLE) animateFrames()
    }

    private fun animateFrames() {
        if (frames?.isActive == true) return
        frames =
            scope?.launch {
                while (isActive) {
                    val progress =
                        ((SystemClock.elapsedRealtime() - started) % SpinGeometry.CYCLE_MILLIS).toDouble() / SpinGeometry.CYCLE_MILLIS
                    levels = withContext(Dispatchers.Default) { SpinGeometry.render(SpinGeometry.yaw(progress)) }
                    invalidate()
                    delay(FRAME_MILLIS)
                }
            }
    }

    override fun onDetachedFromWindow() {
        scope?.cancel()
        scope = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(WhDefaults.oledBackground.toArgb())
        if (width == 0 || height == 0) return
        val pitch = min(width * 0.55f / SpinGeometry.DOT_WIDTH, height * 0.4f / SpinGeometry.DOT_HEIGHT)
        // Una deriva lenta para no marcar siempre los mismos píxeles de una pantalla OLED.
        val drift = (SystemClock.elapsedRealtime() - started) / 60000f
        val left = (width - SpinGeometry.DOT_WIDTH * pitch) / 2 + sin(drift) * width * 0.025f
        val top = (height - SpinGeometry.DOT_HEIGHT * pitch) / 2 + sin(drift * 0.7f) * height * 0.025f
        drawDots(canvas, left, top, pitch)
    }

    private fun drawDots(
        canvas: Canvas,
        left: Float,
        top: Float,
        pitch: Float,
    ) {
        val frame = levels
        counts.fill(0)
        for (index in frame.indices) {
            val level = frame[index]
            if (level == 0) continue
            val n = counts[level]
            points[level][n] = left + (index % SpinGeometry.DOT_WIDTH + 0.5f) * pitch
            points[level][n + 1] = top + (index / SpinGeometry.DOT_WIDTH + 0.5f) * pitch
            counts[level] = n + 2
        }
        dotPaint.strokeWidth = pitch * DOT_SIZE
        for (level in 1..SpinGeometry.LEVELS) {
            if (counts[level] == 0) continue
            dotPaint.color = shade(accent, level.toFloat() / SpinGeometry.LEVELS)
            canvas.drawPoints(points[level], 0, counts[level], dotPaint)
        }
    }

    private fun shade(
        color: Color,
        intensity: Float,
    ): Int = Color(color.red * intensity, color.green * intensity, color.blue * intensity).toArgb()

    companion object {
        private const val FRAME_MILLIS = 33L

        /** Diámetro de un punto braille respecto al paso de la rejilla. */
        private const val DOT_SIZE = 0.62f
    }
}
