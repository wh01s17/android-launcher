package com.wh01s17.launcher.feature.bar.module

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.wh01s17.launcher.core.config.BarModuleConfig
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.feature.bar.BarState

/** Lo que recibe un módulo para pintarse: su entrada de la config, el estado y cómo emitir comandos. */
@Immutable
class BarModuleContext(
    val settings: BarModuleConfig,
    val state: BarState,
    val onCommand: (LauncherCommand) -> Unit,
)

/**
 * Un módulo de la barra (plan.md §5.5), igual que los de `shell.json`: se identifica por `id` y se
 * pinta con [Content]. Un módulo sin nada que mostrar (p. ej. la batería en una tablet sin
 * batería) no emite nada y la barra no le reserva sitio.
 */
interface BarModule {
    val id: String

    @Composable
    fun Content(context: BarModuleContext)
}

/** Los módulos conocidos por `id`. Uno desconocido se pinta como aviso, no se ignora en silencio. */
class BarModuleRegistry(
    modules: List<BarModule>,
) {
    private val byId = modules.associateBy { it.id }

    init {
        require(byId.size == modules.size) { "duplicate bar module ids: ${modules.groupBy { it.id }.filterValues { it.size > 1 }.keys}" }
    }

    fun find(id: String): BarModule? = byId[id]

    val ids: Set<String> get() = byId.keys
}
