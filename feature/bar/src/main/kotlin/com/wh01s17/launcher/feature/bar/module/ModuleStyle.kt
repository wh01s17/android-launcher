package com.wh01s17.launcher.feature.bar.module

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.wh01s17.launcher.core.config.BarModuleConfig
import com.wh01s17.launcher.core.config.HexColor

/** Cómo se pinta un módulo según sus clases actuales y su configuración. */
@Immutable
data class ModuleStyle(
    val color: Color,
    /** Atenuado al 45 %, como `dimmed` de `WidgetButton.qml`. */
    val dimmed: Boolean,
    /** Enmarcado en `[ ]`, como un segmento de la barra de tmux. */
    val brackets: Boolean,
    val bracketColor: Color,
)

/**
 * Lo que un módulo pinta si su configuración no dice otra cosa. Los colores salen del tema (R6), no
 * del código; `classColors` y `dimClasses` de la config los sustituyen.
 */
@Immutable
data class ModuleDefaults(
    val color: Color,
    val bracketColor: Color,
    val classColors: Map<String, Color> = emptyMap(),
    val dimClasses: Set<String> = emptySet(),
)

/**
 * Reglas de `StatusModule.qml` del shell (plan.md §5.5):
 * - `classColors`: gana la primera clase del módulo (en su orden) que tenga color; si ninguna,
 *   `default`; si tampoco, el color base.
 * - `dimClasses`: si el módulo tiene alguna de esas clases, se atenúa.
 * - `brackets` y `bracketColor`: el marco `[ ]`.
 *
 * La config manda sobre [ModuleDefaults] en cada clave que defina; las demás se heredan.
 */
object ModuleStyleResolver {
    fun resolve(
        settings: BarModuleConfig,
        classes: List<String>,
        defaults: ModuleDefaults,
    ): ModuleStyle {
        val colors = defaults.classColors + configuredColors(settings)
        val color = classes.firstNotNullOfOrNull { colors[it] } ?: colors[DEFAULT_CLASS] ?: defaults.color
        val dim = settings.stringList(DIM_CLASSES)?.toSet() ?: defaults.dimClasses
        return ModuleStyle(
            color = color,
            dimmed = classes.any { it in dim },
            brackets = settings.bool(BRACKETS) ?: false,
            bracketColor = settings.string(BRACKET_COLOR)?.let(::toColor) ?: defaults.bracketColor,
        )
    }

    private fun configuredColors(settings: BarModuleConfig): Map<String, Color> =
        settings
            .stringMap(CLASS_COLORS)
            .orEmpty()
            .mapNotNull { (name, hex) -> toColor(hex)?.let { name to it } }
            .toMap()

    private fun toColor(hex: String): Color? = HexColor.parse(hex)?.let(::Color)

    private const val CLASS_COLORS = "classColors"
    private const val DIM_CLASSES = "dimClasses"
    private const val BRACKETS = "brackets"
    private const val BRACKET_COLOR = "bracketColor"
    private const val DEFAULT_CLASS = "default"
}
