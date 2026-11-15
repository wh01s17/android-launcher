package com.wh01s17.launcher.feature.settings.style

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.wh01s17.launcher.core.config.ScreensaverConfig
import com.wh01s17.launcher.core.designsystem.components.WhButton
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.components.WhTextField
import com.wh01s17.launcher.core.designsystem.components.WhToggle
import com.wh01s17.launcher.core.designsystem.screensaver.ScreensaverColor
import com.wh01s17.launcher.core.designsystem.screensaver.ScreensaverStyle
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.settings.R
import kotlinx.coroutines.delay

/**
 * Estilo → Salvapantallas (`screensaver` de la config): qué se muestra (la W, la hora, un texto o sólo
 * datos), qué líneas van debajo, el texto propio, el color, el brillo, la velocidad y el tamaño, con
 * una miniatura que cambia en vivo ([ScreensaverPreview]). El fondo es siempre negro puro OLED; bajar el
 * brillo enciende menos los píxeles.
 */
@Composable
internal fun ScreensaverOptions(
    config: ScreensaverConfig,
    onChange: (String, String, Any) -> Unit,
    onPreview: () -> Unit,
) {
    fun set(
        key: String,
        value: Any,
    ) = onChange("screensaver", key, value)
    ChoiceSetting(
        label = R.string.style_screensaver_style,
        title = R.string.style_screensaver_style_choose,
        choices = ScreensaverStyle.entries.map { Choice(it.id, it.id, stringResource(styleDescription(it))) },
        current = ScreensaverStyle.byId(config.style).id,
        onSelect = { set("style", it) },
    )
    ScreensaverPreview(config, onPreview)
    WhText(stringResource(R.string.style_screensaver_show), color = WhTheme.colors.accent)
    WhToggle(config.showTime, { set("showTime", it) }, label = stringResource(R.string.style_screensaver_show_time))
    WhToggle(config.showDate, { set("showDate", it) }, label = stringResource(R.string.style_screensaver_show_date))
    WhToggle(config.showBattery, { set("showBattery", it) }, label = stringResource(R.string.style_screensaver_show_battery))
    WhToggle(config.showText, { set("showText", it) }, label = stringResource(R.string.style_screensaver_show_text))
    ScreensaverText(config.text) { set("text", it) }
    WhToggle(config.spinText, { set("spinText", it) }, label = stringResource(R.string.style_screensaver_spin_text))
    WhText(stringResource(R.string.style_screensaver_color), color = WhTheme.colors.accent)
    ColorChoices(ScreensaverColor.byId(config.color)) { set("color", it.id) }
    NumberSetting(R.string.style_screensaver_brightness, config.brightness, ScreensaverConfig.BRIGHTNESS) { set("brightness", it) }
    NumberSetting(R.string.style_screensaver_speed, config.speed, ScreensaverConfig.SPEED) { set("speed", it) }
    NumberSetting(R.string.style_screensaver_size, config.size, ScreensaverConfig.SIZE) { set("size", it) }
    WhText(stringResource(R.string.style_screensaver_oled_hint), color = WhTheme.colors.darkForeground)
}

/** El texto propio: se guarda al terminar de escribir (o con «hecho»), no con cada letra. */
@Composable
private fun ScreensaverText(
    saved: String,
    onSave: (String) -> Unit,
) {
    var text by rememberSaveable(saved) { mutableStateOf(saved) }
    LaunchedEffect(text) {
        if (text == saved) return@LaunchedEffect
        delay(SAVE_DELAY_MILLIS)
        onSave(text)
    }
    WhTextField(
        text,
        { text = it.take(ScreensaverConfig.MAX_TEXT) },
        Modifier.fillMaxWidth(),
        placeholder = stringResource(R.string.style_screensaver_text),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onDone = { if (text != saved) onSave(text) }),
    )
}

/** Un botón por color con su muestra (de la paleta elegida). */
@Composable
private fun ColorChoices(
    current: ScreensaverColor,
    onSelect: (ScreensaverColor) -> Unit,
) {
    val palette = WhTheme.palette
    ScreensaverColor.entries.chunked(COLORS_PER_ROW).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
            row.forEach { color ->
                WhButton({ onSelect(color) }, selected = color == current) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(WhTheme.spacing.iconSize).background(color.of(palette)))
                        WhText(color.id)
                    }
                }
            }
        }
    }
}

private fun styleDescription(style: ScreensaverStyle): Int =
    when (style) {
        ScreensaverStyle.W -> R.string.style_screensaver_style_w
        ScreensaverStyle.Clock -> R.string.style_screensaver_style_clock
        ScreensaverStyle.Text -> R.string.style_screensaver_style_text
        ScreensaverStyle.Minimal -> R.string.style_screensaver_style_minimal
    }

private const val SAVE_DELAY_MILLIS = 800L
private const val COLORS_PER_ROW = 3
