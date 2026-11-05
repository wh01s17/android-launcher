package com.wh01s17.launcher.core.system.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * El espectro de lo que suena en el teléfono (el analizador de tidalamp), con `Visualizer` sobre la
 * mezcla de salida (sesión 0). Android lo ata al permiso de micrófono aunque no se grabe nada:
 * opt-in (plan.md R8); sin él no emite nada.
 */
class AudioSpectrum(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun permitted(): Boolean = appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /** [count] bandas de 0 a 1, en escala logarítmica de frecuencia (como un analizador), suavizadas. */
    fun bands(count: Int): Flow<FloatArray> =
        callbackFlow {
            val visualizer =
                try {
                    Visualizer(0).apply {
                        captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(CAPTURE_SIZE)
                    }
                } catch (ignored: RuntimeException) {
                    // Sin permiso o con el efecto ocupado por otra app: no hay espectro.
                    close()
                    return@callbackFlow
                }
            val smoothed = FloatArray(count)
            visualizer.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        v: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int,
                    ) = Unit

                    override fun onFftDataCapture(
                        v: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int,
                    ) {
                        fft ?: return
                        val fresh = SpectrumMath.bands(fft, count)
                        for (i in 0 until count) smoothed[i] = max(fresh[i], smoothed[i] * DECAY)
                        trySend(smoothed.copyOf())
                    }
                },
                Visualizer.getMaxCaptureRate(),
                false,
                true,
            )
            visualizer.enabled = true
            awaitClose {
                visualizer.enabled = false
                visualizer.release()
            }
        }

    private companion object {
        const val CAPTURE_SIZE = 1024

        /** Cuánto queda de la barra anterior en cada captura: caída suave, como el de tidalamp. */
        const val DECAY = 0.82f
    }
}

/** La parte pura: FFT de `Visualizer` → bandas logarítmicas en 0..1 (se prueba en la JVM). */
object SpectrumMath {
    /**
     * [fft] es el formato de `Visualizer.getFft`: `[re0, reN/2, re1, im1, re2, im2, …]`. Las bandas
     * reparten en escala logarítmica los bins [LOW_BIN]..N/2 con bordes fraccionarios: en los graves
     * una banda es más estrecha que un bin y toma la magnitud interpolada entre los dos vecinos (si no,
     * varias bandas seguidas repetirían el mismo bin y el lado izquierdo se vería plano y a saltos). En
     * las demás manda el pico de sus bins. La magnitud pasa a dB entre [FLOOR_DB] y [CEILING_DB], con
     * una compensación de [TILT_DB_PER_OCTAVE] (la del ruido rosa, como un analizador): sin ella y sin
     * margen sobre 0 dB, los graves de casi cualquier canción quedaban recortados en una línea plana.
     */
    fun bands(
        fft: ByteArray,
        count: Int,
    ): FloatArray {
        val bins = fft.size / 2
        val out = FloatArray(count)
        if (bins <= LOW_BIN + 1 || count == 0) return out
        val magnitude = DoubleArray(bins) { k -> if (k == 0) 0.0 else hypot(fft[2 * k].toDouble(), fft[2 * k + 1].toDouble()) }
        val top = (bins - 1).toDouble()
        for (band in 0 until count) {
            val from = edge(band, count, top)
            val to = edge(band + 1, count, top)
            val center = sqrt(from * to)
            val peak =
                if (to - from < 1.0) {
                    interpolate(magnitude, center)
                } else {
                    (ceil(from).toInt()..floor(to).toInt().coerceAtMost(bins - 1)).maxOf { magnitude[it] }
                }
            if (peak <= 0) continue
            val db = 20 * log10(peak / FULL_SCALE) + TILT_DB_PER_OCTAVE * log2(center / LOW_BIN)
            out[band] = ((db - FLOOR_DB) / (CEILING_DB - FLOOR_DB)).toFloat().coerceIn(0f, 1f)
        }
        return out
    }

    /**
     * Borde fraccionario de la banda [band] entre [LOW_BIN] y [top]: logarítmico con un [LINEAR_MIX]
     * lineal. Puramente logarítmico, con 48 bandas, las 15 primeras se repartían menos de 3 bins
     * (cada bin son unos 43 Hz) y dibujaban un tramo recto; así cada banda tiene datos propios.
     */
    private fun edge(
        band: Int,
        count: Int,
        top: Double,
    ): Double {
        val position = band.toDouble() / count
        val logarithmic = LOW_BIN * (top / LOW_BIN).pow(position)
        val linear = LOW_BIN + (top - LOW_BIN) * position
        return logarithmic * (1 - LINEAR_MIX) + linear * LINEAR_MIX
    }

    private fun interpolate(
        magnitude: DoubleArray,
        at: Double,
    ): Double {
        val low = floor(at).toInt().coerceIn(0, magnitude.size - 1)
        val high = (low + 1).coerceAtMost(magnitude.size - 1)
        val t = at - low
        return magnitude[low] * (1 - t) + magnitude[high] * t
    }

    /** Desde el bin 2: el 0 es la continua y el 1 (unos 43 Hz) casi siempre satura. */
    private const val LOW_BIN = 2.0
    private const val FULL_SCALE = 128.0
    private const val FLOOR_DB = -36.0
    private const val CEILING_DB = 12.0
    private const val TILT_DB_PER_OCTAVE = 3.0
    private const val LINEAR_MIX = 0.15
}
