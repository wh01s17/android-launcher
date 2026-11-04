package com.wh01s17.launcher.core.config

import com.wh01s17.launcher.core.tiling.TilingLayout
import com.wh01s17.launcher.core.tiling.Workspace
import kotlin.math.roundToInt

/**
 * `general` de `launcher.json` (plan.md §6.1): el `general` de `looknfeel.lua` más el layout. Las
 * medidas van en dp; `null` toma la del tema (`gapsIn 2`, `gapsOut 2`, `borderSize 1`, `rounding
 * 10`, plan.md §4.5), así un cambio de tema sigue llegando a quien no las fijó.
 */
data class GeneralConfig(
    /** Layout de los workspaces que aún no tienen uno propio (los vacíos). */
    val layout: TilingLayout = TilingLayout.Dwindle,
    /** Huecos por workspace; con todos ocupados, un tile nuevo entra en el grupo del foco. */
    val maxTilesPerWorkspace: Int = Workspace.DEFAULT_MAX_TILES,
    val gapsIn: Int? = null,
    val gapsOut: Int? = null,
    val borderSize: Int? = null,
    val rounding: Int? = null,
) {
    companion object {
        val MAX_TILES = 1..16
        val GAPS = 0..32
        val BORDER = 0..8
        val ROUNDING = 0..32
    }
}

/** `animations` de `launcher.json`, como `hl.animation` de `looknfeel.lua`. */
data class AnimationsConfig(
    val workspaces: WorkspaceAnimation = WorkspaceAnimation(),
)

/** `animations.workspaces`: cambio de escritorio. Sólo existe el estilo `slide`. */
data class WorkspaceAnimation(
    val enabled: Boolean = true,
    val curve: AnimationCurve = AnimationCurve.EaseOutQuint,
    /** En décimas de segundo, como `speed` de Hyprland: 2 = 200 ms. */
    val speed: Float = DEFAULT_SPEED,
) {
    val durationMillis: Int get() = if (enabled) (speed * MILLIS_PER_SPEED).roundToInt() else 0

    companion object {
        const val DEFAULT_SPEED = 2f
        const val MAX_SPEED = 20f
        const val STYLE = "slide"
        private const val MILLIS_PER_SPEED = 100
    }
}

/** Las curvas que define `looknfeel.lua` (`hl.curve`), con sus puntos de control copiados tal cual. */
@Suppress("MagicNumber") // Los puntos son datos de `looknfeel.lua`, no constantes con nombre propio.
enum class AnimationCurve(
    val configName: String,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
) {
    EaseOutQuint("easeOutQuint", 0.23f, 1f, 0.32f, 1f),
    EaseInOutCubic("easeInOutCubic", 0.65f, 0.05f, 0.36f, 1f),
    Linear("linear", 0f, 0f, 1f, 1f),
    AlmostLinear("almostLinear", 0.5f, 0.5f, 0.75f, 1f),
    Quick("quick", 0.15f, 0f, 0.1f, 1f),
    ;

    companion object {
        fun fromConfig(name: String): AnimationCurve? = entries.firstOrNull { it.configName == name }
    }
}
