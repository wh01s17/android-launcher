package com.wh01s17.launcher.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

/**
 * La app medida: la release (`com.wh01s17.launcher`), instalada junto a la del uso diario (`.debug`).
 * No es el HOME del teléfono, así que matarla no apaga los gestos wh01s17 (plan.md §5.9).
 */
const val TARGET_PACKAGE = "com.wh01s17.launcher"

/**
 * HyperOS no entrega a la app medida los broadcasts de `profileinstaller` (instalar el perfil, vaciar la
 * caché de shaders): con `-e wh01s17.manualCompilation true` la compilación la deja hecha quien lanza las
 * pruebas (`cmd package compile`, docs/performance.md) y Macrobenchmark no la toca.
 */
@OptIn(ExperimentalMacrobenchmarkApi::class)
fun compilation(wanted: CompilationMode): CompilationMode =
    if (InstrumentationRegistry.getArguments().getString("wh01s17.manualCompilation") == "true") CompilationMode.Ignore() else wanted

// Sin TalkBack el inicio no publica nodos de accesibilidad (`screenReaderSemantics`), así que
// UiAutomator no los ve: los recorridos van por coordenadas. Se dejan lejos de los bordes, donde están
// las franjas de la capa 3 del launcher diario (atrás a los lados, inicio abajo).
private const val CENTER = 0.5f
private const val SWIPE_FROM = 0.8f
private const val SWIPE_TO = 0.2f
private const val MENU_FROM = 0.7f
private const val MENU_TO = 0.3f

// Pasos de 5 ms de UiDevice.swipe: unos 100 ms por deslizamiento, como un dedo rápido.
private const val SWIPE_STEPS = 20
private const val WORKSPACE_SWIPES = 3
private const val SETTLE_MILLIS = 500L

/** Escritorio 1 → 4 y vuelta, deslizando en horizontal por el centro de la pantalla. */
fun MacrobenchmarkScope.swipeWorkspaces() {
    repeat(WORKSPACE_SWIPES) { device.swipeHorizontal(SWIPE_FROM, SWIPE_TO) }
    repeat(WORKSPACE_SWIPES) { device.swipeHorizontal(SWIPE_TO, SWIPE_FROM) }
}

/** Abre el menú wh01s17 con el gesto de fábrica (deslizar hacia arriba) y lo cierra con *atrás*. */
fun MacrobenchmarkScope.openAndCloseMenu(query: String? = null) {
    val x = (device.displayWidth * CENTER).toInt()
    device.swipe(x, (device.displayHeight * MENU_FROM).toInt(), x, (device.displayHeight * MENU_TO).toInt(), SWIPE_STEPS)
    device.waitForIdle()
    if (query != null) {
        // Escribir en la búsqueda recorre el índice difuso y la lista de resultados.
        device.executeShellCommand("input text $query")
        device.waitForIdle()
    }
    device.pressBack()
    device.waitForIdle()
}

private fun UiDevice.swipeHorizontal(
    from: Float,
    to: Float,
) {
    val y = (displayHeight * CENTER).toInt()
    swipe((displayWidth * from).toInt(), y, (displayWidth * to).toInt(), y, SWIPE_STEPS)
    waitForIdle()
    Thread.sleep(SETTLE_MILLIS)
}
