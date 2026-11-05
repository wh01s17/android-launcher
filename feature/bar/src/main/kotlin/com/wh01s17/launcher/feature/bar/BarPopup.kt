package com.wh01s17.launcher.feature.bar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * El panel que abre un módulo de la barra (calendario, pomodoro, metrónomo): bajo la barra y
 * centrado en la pantalla; se cierra tocando fuera o con *atrás*.
 */
@Composable
internal fun BarPopup(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Popup(
        popupPositionProvider = remember { BelowBarPositionProvider },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
        content = content,
    )
}

/** Bajo la barra y centrado en la ventana, sea cual sea el ancho del módulo que lo abre. */
private object BelowBarPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset((windowSize.width - popupContentSize.width) / 2, anchorBounds.bottom)
}
