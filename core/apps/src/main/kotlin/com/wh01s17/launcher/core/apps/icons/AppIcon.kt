package com.wh01s17.launcher.core.apps.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.wh01s17.launcher.core.apps.model.AppEntry

/**
 * Icono de una app. Si ya está en la caché se pinta en el primer fotograma; si no, el hueco queda
 * vacío hasta que el cargador lo genera fuera del hilo principal. El monocromo se tiñe con [tint]
 * (`foreground` del tema, plan.md §4.7); quien llama pasa los valores del tema (R6).
 */
@Composable
fun AppIcon(
    app: AppEntry,
    loader: AppIconLoader,
    size: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
    style: IconStyle = IconStyle.Default,
) {
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val tint = tint.toArgb()
    val request = remember(app.key, app.iconStamp, style, sizePx, tint) { IconRequest(app, style, sizePx, tint) }
    val bitmap: ImageBitmap? by produceState(loader.cached(request)?.asImageBitmap(), request) {
        value = loader.load(request)?.asImageBitmap()
    }
    Box(modifier.size(size)) {
        bitmap?.let {
            // El pixelado se amplía sin suavizar; el resto ya viene a su tamaño.
            val quality = if (style == IconStyle.Pixelated) FilterQuality.None else FilterQuality.Low
            Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), filterQuality = quality)
        }
    }
}
