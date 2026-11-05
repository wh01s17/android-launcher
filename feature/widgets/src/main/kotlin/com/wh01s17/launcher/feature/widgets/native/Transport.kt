package com.wh01s17.launcher.feature.widgets.native

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

/** Los símbolos de transporte de tidalamp: ◀◀ ▶ ▮▮ ■ ▶▶. */
enum class Transport { Previous, Play, Pause, Stop, Next }

/**
 * Un símbolo de transporte dibujado, todos con la misma caja y el mismo peso visual (los glifos
 * pixel de la fuente tenían tamaños muy distintos: ⏭ salía diminuto y ■ enorme).
 */
@Composable
internal fun TransportGlyph(
    kind: Transport,
    tint: Color,
) {
    Box(
        Modifier.size(WhTheme.spacing.iconSize).drawBehind {
            val h = size.height * HEIGHT
            val top = (size.height - h) / 2
            when (kind) {
                Transport.Play -> triangle(tint, left = (size.width - h * TRIANGLE_RATIO) / 2, top = top, h = h, forward = true)
                Transport.Pause -> {
                    val bar = h * BAR
                    val gap = h * GAP
                    val left = (size.width - 2 * bar - gap) / 2
                    drawRect(tint, Offset(left, top), Size(bar, h))
                    drawRect(tint, Offset(left + bar + gap, top), Size(bar, h))
                }
                Transport.Stop -> {
                    val side = h * STOP
                    drawRect(tint, Offset((size.width - side) / 2, (size.height - side) / 2), Size(side, side))
                }
                Transport.Next, Transport.Previous -> {
                    val w = h * TRIANGLE_RATIO * DOUBLE_SCALE
                    val left = (size.width - 2 * w) / 2
                    val forward = kind == Transport.Next
                    val small = h * DOUBLE_SCALE
                    val y = top + (h - small) / 2
                    triangle(tint, left, y, small, forward)
                    triangle(tint, left + w, y, small, forward)
                }
            }
        },
    )
}

private fun DrawScope.triangle(
    tint: Color,
    left: Float,
    top: Float,
    h: Float,
    forward: Boolean,
) {
    val w = h * TRIANGLE_RATIO
    val path =
        Path().apply {
            if (forward) {
                moveTo(left, top)
                lineTo(left + w, top + h / 2)
                lineTo(left, top + h)
            } else {
                moveTo(left + w, top)
                lineTo(left, top + h / 2)
                lineTo(left + w, top + h)
            }
            close()
        }
    drawPath(path, tint)
}

private const val HEIGHT = 0.72f
private const val TRIANGLE_RATIO = 0.86f
private const val BAR = 0.3f
private const val GAP = 0.26f
private const val STOP = 0.86f
private const val DOUBLE_SCALE = 0.8f
