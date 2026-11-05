package com.wh01s17.launcher.feature.widgets.native

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import java.io.File

/** Frecuencia actual y máxima de un núcleo, en kHz (como `cpufreq`). */
internal data class CoreFrequency(
    val currentKhz: Long,
    val maxKhz: Long,
) {
    val fraction: Float get() = if (maxKhz > 0) currentKhz.toFloat() / maxKhz else 0f
}

/**
 * Lecturas para la vista btop que Android deja hacer sin permisos: la frecuencia de cada núcleo
 * (`/sys/devices/system/cpu/cpuN/cpufreq`) y los bytes de red del teléfono (`TrafficStats`). El uso
 * de CPU (`/proc/stat`) y la carga están vedados a las apps desde Android 8.
 */
internal object SystemSampler {
    private val CPU_DIR = File("/sys/devices/system/cpu")

    fun cores(): List<CoreFrequency> =
        (0 until MAX_CORES).mapNotNull { core ->
            val dir = File(CPU_DIR, "cpu$core/cpufreq")
            val current = read(File(dir, "scaling_cur_freq")) ?: return@mapNotNull null
            CoreFrequency(current, read(File(dir, "cpuinfo_max_freq")) ?: current)
        }

    /** Bytes recibidos y enviados desde el arranque; `null` si el sistema no lo da. */
    fun network(): Pair<Long, Long>? {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        return if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) null else rx to tx
    }

    private fun read(file: File): Long? =
        try {
            file.readText().trim().toLongOrNull()
        } catch (ignored: java.io.IOException) {
            null
        } catch (ignored: SecurityException) {
            null
        }

    private const val MAX_CORES = 16
}

/** Una serie de valores 0..1 de largo fijo: el último a la derecha, como las gráficas de btop. */
internal class History(
    private val size: Int,
) {
    private val values = ArrayDeque<Float>()

    fun add(value: Float): History {
        values.addLast(value.coerceIn(0f, 1f))
        while (values.size > size) values.removeFirst()
        return this
    }

    fun toArray(): FloatArray = FloatArray(size) { i -> values.getOrElse(i - (size - values.size)) { 0f } }
}

/** Memoria, almacenamiento y batería en un instante. */
internal data class Usage(
    val memoryUsed: Long,
    val memoryTotal: Long,
    val storageUsed: Long,
    val storageTotal: Long,
    val batteryPercent: Int?,
    val batteryCelsius: Float?,
    val uptimeMillis: Long,
)

/** Memoria, disco y batería; bloqueante (fuera del hilo principal). Nada pide permisos. */
internal fun readUsage(context: Context): Usage {
    val memory = ActivityManager.MemoryInfo().also { context.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }
    val stat = StatFs(Environment.getDataDirectory().path)
    // La difusión «pegajosa» de la batería se lee sin receptor ni permiso.
    val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val tenths = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
    return Usage(
        memoryUsed = memory.totalMem - memory.availMem,
        memoryTotal = memory.totalMem,
        storageUsed = stat.totalBytes - stat.availableBytes,
        storageTotal = stat.totalBytes,
        batteryPercent = if (level >= 0 && scale > 0) level * PERCENT_INT / scale else null,
        batteryCelsius = tenths.takeIf { it != Int.MIN_VALUE }?.let { it / TENTHS_OF_DEGREE },
        uptimeMillis = SystemClock.elapsedRealtime(),
    )
}

private const val PERCENT_INT = 100
private const val TENTHS_OF_DEGREE = 10f
