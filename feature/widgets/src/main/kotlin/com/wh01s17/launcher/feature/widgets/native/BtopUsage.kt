package com.wh01s17.launcher.feature.widgets.native

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Una muestra de la vista btop, con los historiales ya actualizados. */
private data class BtopSample(
    val usage: Usage,
    val cores: List<CoreFrequency>,
    val rxPerSecond: Long,
    val txPerSecond: Long,
    val cpu: FloatArray,
    val memory: FloatArray,
    val rx: FloatArray,
    val tx: FloatArray,
)

/**
 * El uso del sistema a pantalla completa, al estilo de btop: cajas con el título en el borde, la
 * frecuencia de cada núcleo, historiales de CPU, memoria y red en puntos, disco y batería. Colores
 * en degradado verde → amarillo → rojo según el valor, como btop. Muestrea cada segundo mientras se ve.
 */
@Composable
internal fun BtopUsage(modifier: Modifier) {
    val context = LocalContext.current
    val sample by produceState<BtopSample?>(null) {
        val cpu = History(HISTORY)
        val memory = History(HISTORY)
        val rx = History(HISTORY)
        val tx = History(HISTORY)
        val rates = ArrayDeque<Pair<Long, Long>>()
        var last: Pair<Long, Long>? = null
        while (true) {
            val (usage, cores, net) =
                withContext(
                    Dispatchers.IO,
                ) { Triple(readUsage(context), SystemSampler.cores(), SystemSampler.network()) }
            val rate = if (net != null && last != null) (net.first - last.first) to (net.second - last.second) else 0L to 0L
            last = net
            rates.addLast(rate)
            while (rates.size > HISTORY) rates.removeFirst()
            // Escala automática, como btop: el máximo de lo que se ve en la gráfica.
            val peak = rates.maxOf { maxOf(it.first, it.second) }.coerceAtLeast(MIN_NET_SCALE).toFloat()
            value =
                BtopSample(
                    usage = usage,
                    cores = cores,
                    rxPerSecond = rate.first,
                    txPerSecond = rate.second,
                    cpu = cpu.add(if (cores.isEmpty()) 0f else cores.map { it.fraction }.average().toFloat()).toArray(),
                    memory = memory.add(if (usage.memoryTotal > 0) usage.memoryUsed.toFloat() / usage.memoryTotal else 0f).toArray(),
                    rx = rx.add(rate.first / peak).toArray(),
                    tx = tx.add(rate.second / peak).toArray(),
                )
            delay(SAMPLE_MILLIS)
        }
    }
    val current = sample
    Column(
        modifier.fillMaxSize().background(WhTheme.colors.background).padding(WhTheme.spacing.controlGap),
        verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap),
    ) {
        if (current == null) return@Column
        BtopBox("cpu", "${current.cores.size} cores", Modifier.weight(CPU_WEIGHT)) {
            Cores(current.cores)
            DotGraph(current.cpu, Modifier.weight(1f))
        }
        val usage = current.usage
        BtopBox("mem", gb(usage.memoryUsed, usage.memoryTotal), Modifier.weight(1f)) {
            DotGraph(current.memory, Modifier.weight(1f))
        }
        BtopBox("net", "↓ ${rate(current.rxPerSecond)}  ↑ ${rate(current.txPerSecond)}", Modifier.weight(1f)) {
            DotGraph(current.rx, Modifier.weight(1f), WhTheme.colors.accent)
            DotGraph(current.tx, Modifier.weight(1f), WhTheme.colors.cyan)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
            val disk = if (usage.storageTotal > 0) usage.storageUsed.toFloat() / usage.storageTotal else 0f
            BtopBox("disk", gb(usage.storageUsed, usage.storageTotal), Modifier.weight(1f), fill = false) { Bar(disk) }
            val battery = usage.batteryPercent
            BtopBox("bat", usage.batteryCelsius?.let { "%.1f °C".format(it) }.orEmpty(), Modifier.weight(1f), fill = false) {
                // En la batería lo malo es lo bajo: el degradado va al revés.
                if (battery != null) Bar(battery / PERCENT, color = btopColor(1f - battery / PERCENT), label = "$battery %")
            }
        }
    }
}

/** Una caja de btop: borde redondeado `muted` con el título en acento y [trailing] apagado encima. */
@Composable
private fun BtopBox(
    title: String,
    trailing: String,
    modifier: Modifier,
    /** `true` si la caja tiene una altura dada (peso): su contenido puede rellenarla con gráficas. */
    fill: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = WhTheme.colors
    val spacing = WhTheme.spacing
    var titleHeight by remember { mutableIntStateOf(0) }
    Box(modifier.fillMaxWidth()) {
        Box(
            Modifier.matchParentSize().drawBehind {
                val top = titleHeight / 2f
                drawRoundRect(
                    colors.muted,
                    topLeft = Offset(0f, top),
                    size = Size(size.width, size.height - top),
                    cornerRadius = CornerRadius(spacing.controlGap.toPx()),
                    style = Stroke(spacing.borderSize.toPx()),
                )
            },
        )
        Column(Modifier.fillMaxWidth().then(if (fill) Modifier.fillMaxHeight() else Modifier)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = spacing.controlGap * 2).onSizeChanged { titleHeight = it.height }) {
                WhText(
                    " $title ",
                    Modifier.background(colors.background),
                    style = WhTheme.typography.caption,
                    color = colors.accent,
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                if (trailing.isNotEmpty()) {
                    WhText(
                        " $trailing ",
                        Modifier.background(colors.background),
                        style = WhTheme.typography.caption,
                        color = colors.darkForeground,
                        maxLines = 1,
                    )
                }
            }
            Column(
                (if (fill) Modifier.weight(1f) else Modifier)
                    .padding(start = spacing.controlGap, end = spacing.controlGap, bottom = spacing.controlGap),
                verticalArrangement = Arrangement.spacedBy(spacing.controlGap / 2),
                content = content,
            )
        }
    }
}

/** Un núcleo por fila en dos columnas: «C0 ▮▮▮▮▯▯ 1.9», con el degradado de btop. */
@Composable
private fun Cores(cores: List<CoreFrequency>) {
    for (pair in cores.withIndex().chunked(2)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.panelGap)) {
            for ((index, core) in pair) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    WhText(
                        "C$index",
                        Modifier.width(WhTheme.spacing.iconSizeLarge),
                        style = WhTheme.typography.caption,
                        color = WhTheme.colors.darkForeground,
                    )
                    Bar(core.fraction, Modifier.weight(1f))
                    WhText(
                        "%.1f".format(core.currentKhz / KHZ_PER_GHZ),
                        Modifier.width(WhTheme.spacing.iconSizeLarge),
                        style = WhTheme.typography.caption,
                        color = WhTheme.colors.foreground,
                        textAlign = TextAlign.End,
                    )
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun Bar(
    fraction: Float,
    modifier: Modifier = Modifier.fillMaxWidth(),
    color: Color = btopColor(fraction),
    label: String? = null,
) {
    val empty = WhTheme.colors.border
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap),
    ) {
        Box(
            Modifier.weight(1f).height(WhTheme.spacing.controlGap).drawBehind {
                drawRect(empty)
                drawRect(color, size = size.copy(width = size.width * fraction.coerceIn(0f, 1f)))
            },
        )
        if (label != null) WhText(label, style = WhTheme.typography.caption, color = WhTheme.colors.foreground, maxLines = 1)
    }
}

/**
 * Gráfica de historial en puntos, como las de btop en Braille: columnas de puntos hasta el valor,
 * cada punto del color de su altura (o [color], si se da: red de bajada y subida).
 */
@Composable
private fun DotGraph(
    values: FloatArray,
    modifier: Modifier,
    color: Color? = null,
) {
    val unit = WhTheme.spacing.borderSize
    val low = WhTheme.colors.accent
    val mid = WhTheme.colors.yellow
    val high = WhTheme.colors.red
    Box(
        modifier.fillMaxWidth().drawBehind {
            if (values.isEmpty()) return@drawBehind
            val radius = unit.toPx()
            val pitch = radius * DOT_PITCH
            val rows = (size.height / pitch).toInt().coerceAtLeast(1)
            val step = size.width / values.size
            values.forEachIndexed { i, value ->
                val lit = (value * rows).toInt()
                for (row in 0 until lit) {
                    val level = row.toFloat() / rows
                    val tint = color ?: gradient(level, low, mid, high)
                    drawCircle(tint, radius, Offset(step * (i + HALF), size.height - pitch * (row + HALF)))
                }
            }
        },
    )
}

@Composable
private fun btopColor(value: Float): Color = gradient(value, WhTheme.colors.accent, WhTheme.colors.yellow, WhTheme.colors.red)

private fun gradient(
    value: Float,
    low: Color,
    mid: Color,
    high: Color,
): Color =
    when {
        value < MID -> low
        value < HIGH -> mid
        else -> high
    }

private fun gb(
    used: Long,
    total: Long,
) = "%.1f/%.1f GB".format(used / BYTES_PER_GB, total / BYTES_PER_GB)

/** «12 KB/s», «3.4 MB/s». */
private fun rate(bytes: Long): String =
    when {
        bytes >= BYTES_PER_MB -> "%.1f MB/s".format(bytes / BYTES_PER_MB.toDouble())
        bytes >= BYTES_PER_KB -> "${bytes / BYTES_PER_KB} KB/s"
        else -> "$bytes B/s"
    }

private const val HISTORY = 60
private const val SAMPLE_MILLIS = 1_000L
private const val MIN_NET_SCALE = 64_000L
private const val CPU_WEIGHT = 1.4f
private const val PERCENT = 100f
private const val KHZ_PER_GHZ = 1_000_000.0
private const val BYTES_PER_GB = 1_000_000_000.0
private const val BYTES_PER_KB = 1_000L
private const val BYTES_PER_MB = 1_000_000L
private const val DOT_PITCH = 2.6f
private const val HALF = 0.5f
private const val MID = 0.6f
private const val HIGH = 0.85f
