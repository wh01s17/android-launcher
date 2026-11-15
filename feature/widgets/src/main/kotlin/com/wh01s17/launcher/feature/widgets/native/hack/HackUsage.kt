package com.wh01s17.launcher.feature.widgets.native.hack

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.icons.WhMark
import com.wh01s17.launcher.core.designsystem.layout.WidgetSize
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.widgets.native.CoreFrequency
import com.wh01s17.launcher.feature.widgets.native.SystemSampler
import com.wh01s17.launcher.feature.widgets.native.Usage
import com.wh01s17.launcher.feature.widgets.native.readUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Una lectura del monitor: memoria, disco, batería, núcleos y bytes de red desde el arranque. */
private class Sample(
    val usage: Usage,
    val cores: List<CoreFrequency>,
    val network: Pair<Long, Long>?,
)

/**
 * El monitor hack: `sysmon` (en pantalla completa, encabezado de `neofetch`). Memoria, disco y batería
 * en barras ASCII (la batería en rojo con poca carga); en mediano, el tiempo encendido; en grande, un
 * núcleo por línea con su frecuencia y lo que ha pasado por la red. Relee cada 2 s mientras se ve;
 * nada pide permisos.
 */
@Composable
internal fun HackUsage(modifier: Modifier) {
    val context = LocalContext.current
    val sample by produceState<Sample?>(null) {
        while (true) {
            value = withContext(Dispatchers.IO) { Sample(readUsage(context), SystemSampler.cores(), SystemSampler.network()) }
            delay(REFRESH_MILLIS)
        }
    }
    val colors = WhTheme.colors
    HackFrame(modifier) { size ->
        val current = sample
        if (size == WidgetSize.Full) {
            HackPrompt("neofetch")
            current?.let { Neofetch(it) }
        }
        if (size != WidgetSize.Small) HackPrompt("sysmon")
        if (current == null) return@HackFrame
        val usage = current.usage
        HackMeter("mem ", fraction(usage.memoryUsed, usage.memoryTotal), percent(usage.memoryUsed, usage.memoryTotal), colors.accent)
        HackMeter("disk", fraction(usage.storageUsed, usage.storageTotal), percent(usage.storageUsed, usage.storageTotal), colors.accent)
        usage.batteryPercent?.let { battery ->
            val temp = usage.batteryCelsius?.let { " %.0f°C".format(it) }.orEmpty()
            HackMeter("bat ", battery / PERCENT, "$battery%$temp", if (battery < LOW_BATTERY) colors.red else colors.accent)
        }
        if (size == WidgetSize.Small) return@HackFrame
        if (size >= WidgetSize.Large) {
            HackLine("ram", "${gb(usage.memoryUsed)}/${gb(usage.memoryTotal)}")
            HackLine("disk", "${gb(usage.storageUsed)}/${gb(usage.storageTotal)}")
            current.cores.forEachIndexed { index, core ->
                HackMeter("c$index".padEnd(CORE_LABEL), core.fraction, "%.1fG".format(core.currentKhz / KHZ_PER_GHZ), colors.cyan)
            }
            current.network?.let { (rx, tx) -> HackLine("net", "rx ${bytes(rx)} · tx ${bytes(tx)}") }
        }
        HackLine("up", uptime(usage.uptimeMillis))
    }
}

/**
 * Como `neofetch`: la «W» de wh01s17 de logo, a su lado el equipo (Android, modelo, núcleo, tiempo
 * encendido, CPU y memoria) y debajo la fila de colores de la paleta.
 */
@Composable
private fun Neofetch(sample: Sample) {
    val colors = WhTheme.colors
    val spacing = WhTheme.spacing
    val usage = sample.usage
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.panelGap), verticalAlignment = Alignment.CenterVertically) {
        WhMark(height = spacing.touchTarget * 2, color = colors.accent)
        Column(verticalArrangement = Arrangement.spacedBy(spacing.controlGap / 2)) {
            val title = "wh01s17@${Build.DEVICE.lowercase()}"
            WhText(title, style = WhTheme.typography.bodySmall, color = colors.accent, maxLines = 1)
            WhText("-".repeat(title.length), style = WhTheme.typography.bodySmall, color = colors.darkForeground, maxLines = 1)
            HackLine("os", "Android ${Build.VERSION.RELEASE}")
            HackLine("host", Build.MODEL)
            HackLine("kernel", System.getProperty("os.version").orEmpty().substringBefore('-'))
            HackLine("uptime", uptime(usage.uptimeMillis))
            HackLine("cpu", "${sample.cores.size} cores${socModel()}")
            HackLine("memory", "${gb(usage.memoryUsed)}/${gb(usage.memoryTotal)}")
        }
    }
    // Los cuadros de colores de neofetch, con los de la paleta elegida.
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.controlGap / 2)) {
        listOf(colors.red, colors.yellow, colors.accent, colors.cyan, colors.blue, colors.magenta, colors.foreground, colors.muted)
            .forEach { Box(Modifier.size(spacing.iconSize).background(it)) }
    }
}

/** ` · SM7435`: el chip, si Android lo dice (API 31+). */
private fun socModel(): String =
    if (Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.S
    ) {
        Build.SOC_MODEL
            .takeIf { it.isNotBlank() && it != Build.UNKNOWN }
            ?.let { " · $it" }
            .orEmpty()
    } else {
        ""
    }

private fun fraction(
    used: Long,
    total: Long,
) = if (total > 0) used.toFloat() / total else 0f

private fun percent(
    used: Long,
    total: Long,
) = "%d%%".format((fraction(used, total) * PERCENT).toInt())

private fun gb(bytes: Long) = "%.1fG".format(bytes / BYTES_PER_GB)

/** `812M`, `3.4G`: como `ls -h`. */
private fun bytes(bytes: Long): String =
    when {
        bytes >= BYTES_PER_GB -> "%.1fG".format(bytes / BYTES_PER_GB)
        else -> "%dM".format((bytes / BYTES_PER_MB).toLong())
    }

/** `3d 04:11` o `04:11`, como `uptime`. */
private fun uptime(millis: Long): String {
    val minutes = millis / MILLIS_PER_MINUTE
    val days = minutes / MINUTES_PER_DAY
    val clock = "%02d:%02d".format((minutes % MINUTES_PER_DAY) / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
    return if (days > 0) "${days}d $clock" else clock
}

private const val REFRESH_MILLIS = 2_000L
private const val PERCENT = 100f
private const val LOW_BATTERY = 20
private const val CORE_LABEL = 4
private const val KHZ_PER_GHZ = 1_000_000f
private const val BYTES_PER_GB = 1_073_741_824f
private const val BYTES_PER_MB = 1_048_576f
private const val MILLIS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 1_440L
