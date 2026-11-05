package com.wh01s17.launcher.core.designsystem.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wh01s17.launcher.core.designsystem.theme.LocalWhContentColor
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

/**
 * La «W» de wh01s17, la del botón del menú en la barra del escritorio
 * (`shell/plugins/menu/BarWidget.qml` de los dotfiles): tres barras y dos puentes, sólo el trazo.
 * Mide de alto el cuerpo de letra × 1,25, como allí.
 */
@Composable
fun WhMark(
    modifier: Modifier = Modifier,
    height: Dp = markHeight(),
    color: Color = LocalWhContentColor.current,
    contentDescription: String? = null,
) {
    val path = remember { PathParser().parsePathString(MARK_PATH).toPath() }
    val semantics =
        if (contentDescription != null) {
            Modifier.semantics { this.contentDescription = contentDescription }
        } else {
            Modifier.clearAndSetSemantics { }
        }
    Canvas(modifier.size(height * VIEW_WIDTH / VIEW_HEIGHT, height).then(semantics)) {
        val stroke = MARK_STROKE.toPx()
        // El trazo va centrado en el borde del dibujo: se encoge medio trazo por lado para no cortarlo.
        val unit = (size.height - stroke) / VIEW_HEIGHT
        translate(stroke / 2, stroke / 2) {
            scale(unit, unit, pivot = Offset.Zero) {
                drawPath(path, color, style = Stroke(width = stroke / unit, join = StrokeJoin.Miter))
            }
        }
    }
}

/** Alto por defecto: el cuerpo de letra × 1,25 (`markHeight` en el escritorio). */
@Composable
private fun markHeight(): Dp = with(LocalDensity.current) { (WhTheme.typography.body.fontSize * HEIGHT_FACTOR).toDp() }

/** El trazado del escritorio, en un lienzo de [VIEW_WIDTH] × [VIEW_HEIGHT]. */
private const val MARK_PATH = "M0 0h80v240H0zm128 0h80v240h-80zm128 0h80v240h-80zM64 240h80v64H64zm128 0h80v64h-80z"
private const val VIEW_WIDTH = 336f
private const val VIEW_HEIGHT = 304f
private const val HEIGHT_FACTOR = 1.25f
private val MARK_STROKE = 1.4.dp
