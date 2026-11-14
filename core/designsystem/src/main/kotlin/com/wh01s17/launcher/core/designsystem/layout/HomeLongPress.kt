package com.wh01s17.launcher.core.designsystem.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration

/**
 * Cuánto hay que mantener pulsado el escritorio (fondo, tiles y widgets) para entrar en edición. El
 * del sistema en HyperOS es de 300 ms y la barra de edición salía sin querer; con un sistema más
 * lento se usa el suyo.
 */
const val HOME_LONG_PRESS_MILLIS = 600L

/** La pulsación larga de [content] dura al menos [HOME_LONG_PRESS_MILLIS]. */
@Composable
fun ProvideHomeLongPress(content: @Composable () -> Unit) {
    val base = LocalViewConfiguration.current
    val slower =
        remember(base) {
            object : ViewConfiguration by base {
                override val longPressTimeoutMillis: Long = maxOf(base.longPressTimeoutMillis, HOME_LONG_PRESS_MILLIS)
            }
        }
    CompositionLocalProvider(LocalViewConfiguration provides slower, content = content)
}
