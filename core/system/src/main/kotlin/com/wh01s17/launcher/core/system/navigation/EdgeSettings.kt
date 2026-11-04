package com.wh01s17.launcher.core.system.navigation

/** Cuánto hay que alejarse del borde para que cuente: más sensible = menos recorrido. */
enum class EdgeSensitivity(
    val distanceFactor: Float,
) {
    Low(LOW_FACTOR),
    Normal(1f),
    High(HIGH_FACTOR),
}

private const val LOW_FACTOR = 1.4f
private const val HIGH_FACTOR = 0.7f

/**
 * Ajustes de la capa 3 (plan.md §5.9 y §7 Hito 6): cada gesto por separado, grosor de las franjas y
 * sensibilidad. Los escribe el launcher (Ajustes → Compatibilidad) y los lee `:nav`.
 */
data class EdgeSettings(
    val backLeft: Boolean = true,
    val backRight: Boolean = true,
    val home: Boolean = true,
    val recents: Boolean = true,
    val sideThicknessDp: Int = DEFAULT_SIDE_DP,
    val bottomThicknessDp: Int = DEFAULT_BOTTOM_DP,
    val sensitivity: EdgeSensitivity = EdgeSensitivity.Normal,
) {
    /**
     * Lo que se aplica de verdad. Con los botones ocultos (modo *gestos wh01s17*) *inicio* no se puede
     * apagar: sería la única salida de una app. Los grosores se ajustan a [THICKNESS_DP].
     */
    fun effective(buttonsHidden: Boolean): EdgeSettings =
        copy(
            home = home || buttonsHidden,
            sideThicknessDp = sideThicknessDp.coerceIn(THICKNESS_DP),
            bottomThicknessDp = bottomThicknessDp.coerceIn(THICKNESS_DP),
        )

    /** Bordes con alguna franja: abajo, si hay *inicio* o *recientes*. */
    val edges: Set<Edge>
        get() =
            buildSet {
                if (backLeft) add(Edge.Left)
                if (backRight) add(Edge.Right)
                if (home || recents) add(Edge.Bottom)
            }

    companion object {
        const val DEFAULT_SIDE_DP = 14
        const val DEFAULT_BOTTOM_DP = 16
        val THICKNESS_DP = 8..32

        /** Lo que cambia un toque en los botones − / + del grosor. */
        const val THICKNESS_STEP_DP = 2
    }
}
