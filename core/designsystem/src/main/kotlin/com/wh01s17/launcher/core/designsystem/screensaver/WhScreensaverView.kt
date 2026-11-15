package com.wh01s17.launcher.core.designsystem.screensaver

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.os.BatteryManager
import android.os.SystemClock
import android.view.View
import androidx.annotation.VisibleForTesting
import com.wh01s17.launcher.core.designsystem.palette.PlayerPalette
import com.wh01s17.launcher.core.designsystem.palette.TidalampPalettes
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

/**
 * El salvapantallas (`screensaver` de la config): la W de puntos de `w-spin.py`, la hora, un texto
 * propio o sólo datos, y debajo las líneas elegidas (hora, fecha, batería, texto). Pensado para OLED:
 * fondo negro puro siempre, el color atenuado según el brillo elegido, una deriva lenta para no marcar
 * siempre los mismos píxeles y pocos fotogramas (30 fps la W; dos por segundo el resto, lo justo para el
 * parpadeo). Los fotogramas de la W se calculan fuera del hilo de UI, y nada corre si no se ve.
 */
@Suppress("MagicNumber") // Tiempos de cámara y encuadre relativo, independientes de la densidad.
class WhScreensaverView(
    context: Context,
    private val palettes: Flow<PlayerPalette>,
    private val settingsFlow: Flow<ScreensaverSettings>,
) : View(context) {
    private var palette = TidalampPalettes.default
    private var settings = ScreensaverSettings()
    private val painter = ScreensaverPainter(context)
    private var frames: Job? = null
    private var scope: CoroutineScope? = null
    private var levels = IntArray(0)
    private var yaw = 0.0
    private val started = SystemClock.elapsedRealtime()
    private var battery: String? = null
    private var batteryReadAt = 0L

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope?.launch {
            palettes.collect {
                palette = it
                invalidate()
            }
        }
        scope?.launch { settingsFlow.collect(::applySettings) }
        animateFrames()
    }

    /** Aplica unos ajustes nuevos; el ritmo de fotogramas depende del estilo, así que reinicia el bucle. */
    @VisibleForTesting
    internal fun applySettings(next: ScreensaverSettings) {
        settings = next
        frames?.cancel()
        if (windowVisibility == VISIBLE && scope != null) animateFrames()
        invalidate()
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
                    // La W y el texto que gira llevan el mismo giro (vueltas con pausas) a 30 fps; el resto
                    // sólo necesita dos fotogramas por segundo para el parpadeo.
                    val spinning = settings.style == ScreensaverStyle.W || (settings.style == ScreensaverStyle.Text && settings.spinText)
                    if (spinning) {
                        val elapsed = ((SystemClock.elapsedRealtime() - started) * settings.speed).toLong()
                        val progress = (elapsed % SpinGeometry.CYCLE_MILLIS).toDouble() / SpinGeometry.CYCLE_MILLIS
                        yaw = SpinGeometry.yaw(progress)
                        if (settings.style == ScreensaverStyle.W) levels = withContext(Dispatchers.Default) { SpinGeometry.render(yaw) }
                    }
                    readBattery()
                    invalidate()
                    delay(if (spinning) FRAME_MILLIS else BLINK_MILLIS)
                }
            }
    }

    override fun onDetachedFromWindow() {
        scope?.cancel()
        scope = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        painter.draw(canvas, width, height, settings, palette, levels, battery, started, yaw)
    }

    /** La batería, como mucho una vez por minuto (la difusión «pegajosa» se lee sin receptor). */
    private fun readBattery() {
        val now = SystemClock.elapsedRealtime()
        if (!settings.showBattery || now - batteryReadAt < BATTERY_MILLIS && battery != null) return
        batteryReadAt = now
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        if (level < 0 || scale <= 0) return
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        battery = "bat ${level * 100 / scale}%" + if (charging) " +" else ""
    }

    companion object {
        private const val FRAME_MILLIS = 33L
        private const val BLINK_MILLIS = 500L
        private const val BATTERY_MILLIS = 60_000L
    }
}
