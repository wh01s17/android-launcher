package com.wh01s17.launcher.core.designsystem.layout

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable

/*
 * Insets del launcher. No se usa `safeDrawing` porque incluye el teclado *animado*: cada fotograma de
 * la animación del IME cambiaba el tamaño de lo que lo usaba. Con los escritorios detrás del menú,
 * eso era medir el mosaico entero y avisar al ViewModel unas 30 veces por apertura: el teclado
 * aparecía «a cámara lenta» en el Redmi.
 */

/** Barras del sistema y recorte de la cámara, sin el teclado: lo que está debajo no cambia de tamaño. */
val WindowInsets.Companion.chrome: WindowInsets
    @Composable get() = systemBars.union(displayCutout)

/**
 * [chrome] más el teclado en su posición final, desde el primer fotograma de su animación: un panel
 * con un campo de texto se ajusta una vez y el teclado entra en el hueco, en vez de rehacerse en
 * cada fotograma.
 */
@OptIn(ExperimentalLayoutApi::class)
val WindowInsets.Companion.chromeAndKeyboard: WindowInsets
    @Composable get() = chrome.union(imeAnimationTarget)
