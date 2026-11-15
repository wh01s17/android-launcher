package com.wh01s17.launcher.feature.settings.style

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.viewinterop.AndroidView
import com.wh01s17.launcher.core.config.ScreensaverConfig
import com.wh01s17.launcher.core.designsystem.modifier.whBorder
import com.wh01s17.launcher.core.designsystem.screensaver.ScreensaverColor
import com.wh01s17.launcher.core.designsystem.screensaver.ScreensaverSettings
import com.wh01s17.launcher.core.designsystem.screensaver.ScreensaverStyle
import com.wh01s17.launcher.core.designsystem.screensaver.WhScreensaverView
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.settings.R
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * El salvapantallas en pequeño, con la proporción de la pantalla, dentro de sus opciones: cambia en
 * vivo con cada ajuste (el texto propio, al guardarse, un momento después de dejar de escribir).
 * Tocarlo abre la vista previa a pantalla completa.
 */
@Composable
internal fun ScreensaverPreview(
    config: ScreensaverConfig,
    onFullScreen: () -> Unit,
) {
    val settings = config.toSettings()
    val palette = WhTheme.palette
    val settingsFlow = remember { MutableStateFlow(settings) }
    val paletteFlow = remember { MutableStateFlow(palette) }
    LaunchedEffect(settings) { settingsFlow.value = settings }
    LaunchedEffect(palette) { paletteFlow.value = palette }
    val screen = LocalConfiguration.current
    val aspect = screen.screenWidthDp.toFloat() / screen.screenHeightDp.coerceAtLeast(1)
    val shape = WhTheme.borders.shape
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { WhScreensaverView(it, paletteFlow, settingsFlow) },
            modifier =
                Modifier
                    .fillMaxWidth(PREVIEW_WIDTH)
                    .aspectRatio(aspect)
                    .clip(shape)
                    .whBorder(active = false, shape = shape, width = WhTheme.spacing.borderSize)
                    .clickable(onClickLabel = stringResource(R.string.style_preview), role = Role.Button, onClick = onFullScreen),
        )
    }
}

/** La sección `screensaver` de la config, en lo que pinta [WhScreensaverView] (porcentajes → fracciones). */
fun ScreensaverConfig.toSettings(): ScreensaverSettings =
    ScreensaverSettings(
        style = ScreensaverStyle.byId(style),
        text = text,
        showTime = showTime,
        showDate = showDate,
        showBattery = showBattery,
        showText = showText,
        spinText = spinText,
        color = ScreensaverColor.byId(color),
        brightness = brightness / PERCENT,
        speed = speed / PERCENT,
        size = size / PERCENT,
    )

/** La miniatura ocupa la mitad del ancho: se ve entera sin tapar las opciones. */
private const val PREVIEW_WIDTH = 0.5f
private const val PERCENT = 100f
