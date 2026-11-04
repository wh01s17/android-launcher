package com.wh01s17.launcher.feature.menu.search

import android.view.KeyEvent
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * HyperOS puede entregar teclas al diálogo sin foco Compose. Interceptamos sólo las teclas
 * del menú en su ventana y restauramos el callback al cerrar, sin tocar la actividad.
 */
@Composable
internal fun DialogKeyboard(onKey: (KeyEvent) -> Boolean) {
    val view = LocalView.current
    val currentHandler by rememberUpdatedState(onKey)
    DisposableEffect(view) {
        val window = (view.parent as? DialogWindowProvider)?.window
        val previous = window?.callback
        if (window == null || previous == null) return@DisposableEffect onDispose {}
        val callback =
            object : Window.Callback by previous {
                override fun dispatchKeyEvent(event: KeyEvent): Boolean = currentHandler(event) || previous.dispatchKeyEvent(event)
            }
        window.callback = callback
        onDispose { if (window.callback === callback) window.callback = previous }
    }
}
