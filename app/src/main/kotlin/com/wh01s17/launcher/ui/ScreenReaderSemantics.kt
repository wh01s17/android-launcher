package com.wh01s17.launcher.ui

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics

/**
 * El árbol de accesibilidad del inicio sólo cuando hay un lector de pantalla (exploración táctil).
 *
 * Compose recalcula los nodos de accesibilidad en cada fotograma de desplazamiento si hay *cualquier*
 * servicio de accesibilidad activo, y el nuestro (`WhNavigationService`, para los gestos) siempre lo
 * está: en el Redmi era un cuarto del hilo principal al deslizar escritorios. Sin TalkBack nadie lee
 * ese árbol, así que se vacía; al activar TalkBack vuelve en el acto.
 */
@Composable
fun screenReaderSemantics(): Modifier {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager }
    var reading by remember(manager) { mutableStateOf(manager.isTouchExplorationEnabled) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { reading = it }
        manager.addTouchExplorationStateChangeListener(listener)
        reading = manager.isTouchExplorationEnabled
        onDispose { manager.removeTouchExplorationStateChangeListener(listener) }
    }
    return if (reading) Modifier else Modifier.clearAndSetSemantics {}
}
