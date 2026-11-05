package com.wh01s17.launcher.feature.widgets.ui

import com.wh01s17.launcher.core.fuzzy.FuzzyIndex
import com.wh01s17.launcher.feature.widgets.host.WidgetProvider
import com.wh01s17.launcher.feature.widgets.native.NativeWidget

/** Una fila del selector: un widget propio o uno del sistema, con su nombre y el de su app. */
internal sealed interface PickerEntry {
    val label: String
    val app: String

    data class Native(
        val widget: NativeWidget,
        override val label: String,
        override val app: String,
    ) : PickerEntry

    data class System(
        val provider: WidgetProvider,
    ) : PickerEntry {
        override val label: String get() = provider.label
        override val app: String get() = provider.appLabel
    }
}

/**
 * La búsqueda del selector, con el mismo motor que el menú (`core/fuzzy`): cada palabra debe estar
 * en el nombre del widget (como subsecuencia, sin tildes ni mayúsculas) o seguida en el de su app.
 * Se construye una vez por lista; [search] sólo ejecuta la búsqueda.
 */
internal class WidgetSearch<T>(
    items: List<T>,
    label: (T) -> String,
    app: (T) -> String,
) {
    private val index = FuzzyIndex(items, label) { listOf(app(it)) }

    /** De mejor a peor; a igual puntuación, en el orden de la lista. */
    fun search(query: String): List<T> = index.search(query).map { it.item }
}
