package com.wh01s17.launcher.core.designsystem.screensaver

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.wh01s17.launcher.core.designsystem.R
import com.wh01s17.launcher.core.designsystem.theme.WhDefaults
import com.wh01s17.launcher.core.designsystem.tokens.WhTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.sin

/** ASCII frames are rendered off the UI thread, only while this view is visible and attached. */
@Suppress("MagicNumber") // Camera timing and relative framing, independent of Android density.
class WhSpinView(
    context: Context,
) : View(context) {
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WhTokens.Colors.accent.toArgb()
            typeface = ResourcesCompat.getFont(context, R.font.departure_mono)
        }
    private var frames: Job? = null
    private var scope: CoroutineScope? = null
    private var lines = emptyList<String>()
    private val started = SystemClock.elapsedRealtime()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
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
                    val yaw = (SystemClock.elapsedRealtime() - started) / 8000.0 * 2 * Math.PI
                    lines = withContext(Dispatchers.Default) { SpinGeometry.render(yaw) }
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
        val cell = min(width * 0.8f / SpinGeometry.WIDTH, height * 0.6f / (SpinGeometry.HEIGHT * 2))
        paint.textSize = cell
        paint.textSize *= cell / paint.measureText("M")
        val drift = (SystemClock.elapsedRealtime() - started) / 60000f
        val left = (width - SpinGeometry.WIDTH * cell) / 2 + sin(drift) * width * 0.025f
        val top = (height - SpinGeometry.HEIGHT * cell * 2) / 2 + sin(drift * 0.7f) * height * 0.025f
        lines.forEachIndexed { index, line -> canvas.drawText(line, left, top + (index + 1) * cell * 2, paint) }
    }

    companion object {
        private const val FRAME_MILLIS = 66L
    }
}
