package com.wh01s17.launcher.core.system.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

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
     * cubren los bins 1..N/2 en escala logarítmica; la magnitud pasa a dB entre [FLOOR_DB] y 0.
     */
    fun bands(
        fft: ByteArray,
        count: Int,
    ): FloatArray {
        val bins = fft.size / 2
        val out = FloatArray(count)
        if (bins < 2 || count == 0) return out
        for (band in 0 until count) {
            val from = edge(band, count, bins)
            val to = max(from + 1, edge(band + 1, count, bins))
            var peak = 0.0
            for (k in from until minOf(to, bins)) {
                val re = fft[2 * k].toDouble()
                val im = fft[2 * k + 1].toDouble()
                peak = max(peak, hypot(re, im))
            }
            val db = if (peak > 0) 20 * log10(peak / FULL_SCALE) else FLOOR_DB
            out[band] = ((db - FLOOR_DB) / -FLOOR_DB).toFloat().coerceIn(0f, 1f)
        }
        return out
    }

    /** Primer bin de la banda [band]: reparto logarítmico entre el bin 1 y [bins]. */
    private fun edge(
        band: Int,
        count: Int,
        bins: Int,
    ): Int = (bins.toDouble().pow(band.toDouble() / count)).toInt().coerceIn(1, bins)

    private const val FULL_SCALE = 128.0
    private const val FLOOR_DB = -48.0
}
