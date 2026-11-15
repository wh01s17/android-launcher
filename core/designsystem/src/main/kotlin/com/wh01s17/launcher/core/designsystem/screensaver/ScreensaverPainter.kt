package com.wh01s17.launcher.core.designsystem.screensaver

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.SystemClock
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.wh01s17.launcher.core.designsystem.R
import com.wh01s17.launcher.core.designsystem.palette.PlayerPalette
import com.wh01s17.launcher.core.designsystem.theme.WhDefaults
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min
import kotlin.math.sin

/**
 * Dibuja un fotograma de [WhScreensaverView]: lo principal según el estilo y las líneas elegidas debajo.
 * Reutiliza sus pinceles y arreglos entre fotogramas para no crear basura.
 */
@Suppress("MagicNumber") // Encuadre relativo al tamaño de la pantalla, independiente de la densidad.
internal class ScreensaverPainter(
    context: Context,
) {
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = runCatching { ResourcesCompat.getFont(context, R.font.departure_mono) }.getOrNull()
            textAlign = Paint.Align.CENTER
        }
    private val blockPaint = Paint()

    /** Coordenadas por nivel de brillo, reutilizadas en cada fotograma. */
    private val points = Array(SpinGeometry.LEVELS + 1) { FloatArray(SpinGeometry.DOT_WIDTH * SpinGeometry.DOT_HEIGHT * 2) }
    private val counts = IntArray(SpinGeometry.LEVELS + 1)

    // Lo del fotograma en curso.
    private var width = 0
    private var height = 0
    private var settings = ScreensaverSettings()
    private var battery: String? = null
    private var levels = IntArray(0)
    private var yaw = 0.0
    private val matrix = Matrix()
    private val source = FloatArray(CORNERS * 2)
    private val target = FloatArray(CORNERS * 2)

    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        settings: ScreensaverSettings,
        palette: PlayerPalette,
        levels: IntArray,
        battery: String?,
        started: Long,
        yaw: Double = 0.0,
    ) {
        this.width = width
        this.height = height
        this.settings = settings
        this.battery = battery
        this.levels = levels
        this.yaw = yaw
        canvas.drawColor(WhDefaults.oledBackground.toArgb())
        if (width == 0 || height == 0) return
        val color = dim(settings.color.of(palette), settings.brightness)
        // Una deriva lenta para no marcar siempre los mismos píxeles de una pantalla OLED.
        val drift = (SystemClock.elapsedRealtime() - started) / 60000f
        val dx = sin(drift) * width * 0.025f
        val dy = sin(drift * 0.7f) * height * 0.025f
        val blink = (System.currentTimeMillis() / BLINK_MILLIS) % 2 == 0L
        val bottom =
            when (settings.style) {
                ScreensaverStyle.W -> drawW(canvas, color, dx, dy)
                ScreensaverStyle.Clock -> drawBig(canvas, clockText(blink), color, dx, dy, cursor = false, blink)
                ScreensaverStyle.Text -> drawBig(canvas, settings.text.ifBlank { DEFAULT_TEXT }, color, dx, dy, cursor = true, blink)
                ScreensaverStyle.Minimal -> height * 0.4f + dy
            }
        drawLines(canvas, dim(color, LINE_DIM), bottom, dx)
    }

    /** La W de puntos; devuelve dónde acaba por abajo. */
    private fun drawW(
        canvas: Canvas,
        color: Color,
        dx: Float,
        dy: Float,
    ): Float {
        val pitch = min(width * 0.55f / SpinGeometry.DOT_WIDTH, height * 0.4f / SpinGeometry.DOT_HEIGHT) * settings.size
        val left = (width - SpinGeometry.DOT_WIDTH * pitch) / 2 + dx
        val top = (height - SpinGeometry.DOT_HEIGHT * pitch) / 2 - lineBlockHeight() / 2 + dy
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
            dotPaint.color = dim(color, level.toFloat() / SpinGeometry.LEVELS).toArgb()
            canvas.drawPoints(points[level], 0, counts[level], dotPaint)
        }
        return top + SpinGeometry.DOT_HEIGHT * pitch
    }

    /** Un texto grande centrado (la hora o el propio), con el cursor de bloque que parpadea si [cursor]. */
    private fun drawBig(
        canvas: Canvas,
        text: String,
        color: Color,
        dx: Float,
        dy: Float,
        cursor: Boolean,
        blink: Boolean,
    ): Float {
        textPaint.textSize = REFERENCE_SIZE
        val measured = textPaint.measureText(if (cursor) "$text█" else text).coerceAtLeast(1f)
        textPaint.textSize = min(REFERENCE_SIZE * width * BIG_WIDTH / measured, width * BIG_MAX) * settings.size
        textPaint.color = color.toArgb()
        val metrics = textPaint.fontMetrics
        val baseline = height * 0.42f + dy - lineBlockHeight() / 2
        val textWidth = textPaint.measureText(text)
        val cursorWidth = if (cursor) textPaint.measureText("█") else 0f
        val centerX = width / 2f + dx - cursorWidth / 2
        val spin = cursor && settings.spinText
        // De canto (90°) el texto no tiene ancho: no se dibuja nada en ese fotograma.
        val visible =
            !spin || spinAround(canvas, centerX, baseline + metrics.ascent, baseline + metrics.descent, (textWidth + cursorWidth * 2) / 2)
        if (visible) canvas.drawText(text, centerX, baseline, textPaint)
        if (visible && cursor && blink) {
            blockPaint.color = color.toArgb()
            val left = centerX + textWidth / 2 + cursorWidth * 0.15f
            canvas.drawRect(left, baseline + metrics.ascent * 0.8f, left + cursorWidth * 0.7f, baseline, blockPaint)
        }
        if (spin) canvas.restore()
        return baseline + metrics.descent
    }

    /**
     * Gira lo que se dibuje después alrededor del eje vertical en [x] (el texto ocupa de [top] a [bottom] y
     * [halfWidth] a cada lado), con el ángulo de la W. Proyecta las cuatro esquinas: el ancho se encoge con
     * el coseno y el lado que se aleja se achica por la perspectiva de una cámara a dos anchos de
     * pantalla. Hay que cerrar con `canvas.restore()`. Devuelve `false` si queda de canto (sin ancho).
     */
    private fun spinAround(
        canvas: Canvas,
        x: Float,
        top: Float,
        bottom: Float,
        halfWidth: Float,
    ): Boolean {
        val y = (top + bottom) / 2
        val distance = width * CAMERA_DISTANCE
        val cos = kotlin.math.cos(yaw).toFloat()
        val sin = kotlin.math.sin(yaw).toFloat()

        // Cada borde vertical: su x proyectada y cuánto se agranda o achica según se acerca o se aleja.
        fun edge(side: Float): Pair<Float, Float> {
            val depth = side * halfWidth * sin
            val scale = distance / (distance + depth)
            return x + side * halfWidth * cos * scale to scale
        }
        val (leftX, leftScale) = edge(-1f)
        val (rightX, rightScale) = edge(1f)
        source[0] = x - halfWidth
        source[1] = top
        source[2] = x + halfWidth
        source[3] = top
        source[4] = x + halfWidth
        source[5] = bottom
        source[6] = x - halfWidth
        source[7] = bottom
        target[0] = leftX
        target[1] = y + (top - y) * leftScale
        target[2] = rightX
        target[3] = y + (top - y) * rightScale
        target[4] = rightX
        target[5] = y + (bottom - y) * rightScale
        target[6] = leftX
        target[7] = y + (bottom - y) * leftScale
        canvas.save()
        if (kotlin.math.abs(rightX - leftX) < 1f || !matrix.setPolyToPoly(source, 0, target, 0, CORNERS)) return false
        canvas.concat(matrix)
        return true
    }

    /** Las líneas elegidas, centradas debajo de lo principal (en el estilo mínimo, más grandes). */
    private fun drawLines(
        canvas: Canvas,
        color: Color,
        top: Float,
        dx: Float,
    ) {
        val lines = lines()
        if (lines.isEmpty()) return
        textPaint.textSize = lineSize()
        textPaint.color = color.toArgb()
        val step = textPaint.fontSpacing
        var baseline = top + step * 1.4f
        for (line in lines) {
            canvas.drawText(line, width / 2f + dx, baseline, textPaint)
            baseline += step
        }
    }

    private fun lines(): List<String> =
        buildList {
            val now = LocalDateTime.now()
            if (settings.showTime && settings.style != ScreensaverStyle.Clock) add(now.format(TIME))
            if (settings.showDate) add(now.format(DateTimeFormatter.ofPattern("EEE dd-MM-yyyy", Locale.getDefault())).lowercase())
            if (settings.showBattery) battery?.let(::add)
            if (settings.showText && settings.style != ScreensaverStyle.Text) add(settings.text)
        }

    private fun lineSize(): Float = width * (if (settings.style == ScreensaverStyle.Minimal) MINIMAL_LINE * settings.size else LINE)

    /** El alto que ocupan las líneas, para centrar lo principal y las líneas juntas. */
    private fun lineBlockHeight(): Float = if (lines().isEmpty()) 0f else lineSize() * 1.3f * (lines().size + 1)

    /** `21:54` con los dos puntos parpadeando cada medio segundo. */
    private fun clockText(blink: Boolean): String = LocalDateTime.now().format(TIME).let { if (blink) it else it.replace(':', ' ') }

    private fun dim(
        color: Color,
        intensity: Float,
    ): Color = Color(color.red * intensity, color.green * intensity, color.blue * intensity)

    companion object {
        private const val BLINK_MILLIS = 500L
        private const val DEFAULT_TEXT = "wh01s17"
        private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        /** Diámetro de un punto braille respecto al paso de la rejilla. */
        private const val DOT_SIZE = 0.62f
        private const val REFERENCE_SIZE = 100f

        /** Lo grande ocupa como mucho el 85 % del ancho y un tamaño de letra del 30 % del ancho. */
        private const val BIG_WIDTH = 0.85f
        private const val BIG_MAX = 0.3f
        private const val LINE = 0.045f
        private const val MINIMAL_LINE = 0.075f
        private const val LINE_DIM = 0.7f

        /** La cámara del giro del texto, a dos anchos de pantalla. */
        private const val CAMERA_DISTANCE = 2f
        private const val CORNERS = 4
    }
}
