package com.wh01s17.launcher.feature.widgets.native

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.widgets.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Memoria y almacenamiento usados y totales, en bytes. */
private data class Usage(
    val memoryUsed: Long,
    val memoryTotal: Long,
    val storageUsed: Long,
    val storageTotal: Long,
)

/**
 * Uso del sistema (plan.md §7 Hito 7, como `system-usage` de la barra del escritorio): memoria y
 * almacenamiento con medidores de bloques; amarillo por encima del 80 %. Se relee cada 5 s mientras
 * se ve.
 */
@Composable
internal fun UsageWidget(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val usage by produceState<Usage?>(null) {
        while (true) {
            value = withContext(Dispatchers.IO) { read(context) }
            delay(REFRESH_MILLIS)
        }
    }
    Column(
        modifier.fillMaxSize().background(WhTheme.colors.background).padding(WhTheme.spacing.controlGap),
        // Arriba, pegado al encabezado, como el widget de TIDAL.
        verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap),
    ) {
        NovaHeading(stringResource(R.string.native_usage_title))
        val current = usage ?: return@Column
        Meter(stringResource(R.string.native_usage_memory), current.memoryUsed, current.memoryTotal)
        Meter(stringResource(R.string.native_usage_storage), current.storageUsed, current.storageTotal)
    }
}

@Composable
private fun Meter(
    label: String,
    used: Long,
    total: Long,
) {
    val fraction = if (total > 0) used.toFloat() / total else 0f
    RetroMeter(
        label,
        fraction,
        "${gigabytes(used)}/${gigabytes(total)} GB",
        color = if (fraction > WARN_FRACTION) WhTheme.colors.yellow else WhTheme.colors.accent,
    )
}

private fun read(context: Context): Usage {
    val memory = ActivityManager.MemoryInfo().also { context.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }
    val stat = StatFs(Environment.getDataDirectory().path)
    return Usage(
        memoryUsed = memory.totalMem - memory.availMem,
        memoryTotal = memory.totalMem,
        storageUsed = stat.totalBytes - stat.availableBytes,
        storageTotal = stat.totalBytes,
    )
}

private fun gigabytes(bytes: Long) = "%.1f".format(bytes / BYTES_PER_GB)

private const val REFRESH_MILLIS = 5_000L
private const val WARN_FRACTION = 0.8f
private const val BYTES_PER_GB = 1_000_000_000.0
