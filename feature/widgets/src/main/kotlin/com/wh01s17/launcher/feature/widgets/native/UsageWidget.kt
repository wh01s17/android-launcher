package com.wh01s17.launcher.feature.widgets.native

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.layout.WidgetSize
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.widgets.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Uso del sistema (plan.md §7 Hito 7, como `system-usage` de la barra del escritorio): RAM, disco y
 * batería con barras alineadas y su porcentaje; amarillo por encima del 80 % (batería: por debajo
 * del 20 %). En grande, además, los GB, la temperatura de la batería y el tiempo encendido. Se
 * relee cada 5 s mientras se ve; nada pide permisos.
 */
@Composable
internal fun UsageWidget(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        // Casi a pantalla completa, la vista de btop; si no, la compacta.
        if (WidgetSize.of(maxWidth, maxHeight) ==
            WidgetSize.Full
        ) {
            BtopUsage(Modifier.fillMaxSize())
        } else {
            CompactUsage(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun CompactUsage(modifier: Modifier) {
    val context = LocalContext.current
    val usage by produceState<Usage?>(null) {
        while (true) {
            value = withContext(Dispatchers.IO) { readUsage(context) }
            delay(REFRESH_MILLIS)
        }
    }
    BoxWithConstraints(modifier.fillMaxSize().background(WhTheme.colors.background).padding(WhTheme.spacing.controlGap)) {
        val size = WidgetSize.of(maxWidth, maxHeight)
        Column(verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
            if (size != WidgetSize.Small) NovaHeading(stringResource(R.string.native_usage_title))
            val current = usage ?: return@Column
            val detail = size == WidgetSize.Large
            Meter("RAM", current.memoryUsed, current.memoryTotal, if (detail) gb(current.memoryUsed, current.memoryTotal) else null)
            Meter("DISK", current.storageUsed, current.storageTotal, if (detail) gb(current.storageUsed, current.storageTotal) else null)
            current.batteryPercent?.let { percent ->
                val warn = percent < LOW_BATTERY
                val temp = current.batteryCelsius?.let { "%.1f °C".format(it) }
                PercentRow("BAT", percent / PERCENT, warn, if (detail) temp else null)
            }
            if (detail) {
                WhText(
                    stringResource(R.string.native_usage_uptime, uptime(current.uptimeMillis)),
                    style = WhTheme.typography.caption,
                    color = WhTheme.colors.darkForeground,
                )
            }
        }
    }
}

@Composable
private fun Meter(
    label: String,
    used: Long,
    total: Long,
    detail: String?,
) {
    val fraction = if (total > 0) used.toFloat() / total else 0f
    PercentRow(label, fraction, fraction > WARN_FRACTION, detail)
}

/** Etiqueta y porcentaje con ancho fijo, para que todas las barras empiecen y acaben igual. */
@Composable
private fun PercentRow(
    label: String,
    fraction: Float,
    warn: Boolean,
    detail: String?,
) {
    val colors = WhTheme.colors
    val column = WhTheme.spacing.touchTarget
    val color: Color = if (warn) colors.yellow else colors.accent
    Column(verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap / 2)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WhText(label, Modifier.width(column), style = WhTheme.typography.caption, color = colors.darkForeground, maxLines = 1)
            Box(
                Modifier.weight(1f).height(WhTheme.spacing.controlGap).drawBehind {
                    drawRect(colors.border)
                    drawRect(color, size = size.copy(width = size.width * fraction.coerceIn(0f, 1f)))
                },
            )
            WhText(
                "${(fraction * PERCENT).toInt()} %",
                Modifier.width(column),
                style = WhTheme.typography.caption,
                color = colors.foreground,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }
        if (detail != null) {
            WhText(
                detail,
                Modifier.padding(start = column),
                style = WhTheme.typography.caption,
                color = colors.darkForeground,
                maxLines = 1,
            )
        }
    }
}

private fun gb(
    used: Long,
    total: Long,
) = "%.1f / %.1f GB".format(used / BYTES_PER_GB, total / BYTES_PER_GB)

/** «3 d 4 h» o «4 h 12 min». */
private fun uptime(millis: Long): String {
    val minutes = millis / MILLIS_PER_MINUTE
    val days = minutes / MINUTES_PER_DAY
    val hours = (minutes % MINUTES_PER_DAY) / MINUTES_PER_HOUR
    return if (days > 0) "$days d $hours h" else "$hours h ${minutes % MINUTES_PER_HOUR} min"
}

private const val REFRESH_MILLIS = 5_000L
private const val WARN_FRACTION = 0.8f
private const val LOW_BATTERY = 20
private const val PERCENT = 100f
private const val TENTHS = 10f
private const val BYTES_PER_GB = 1_000_000_000.0
private const val MILLIS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 1440L
