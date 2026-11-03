package com.wh01s17.launcher.core.system.navigation

/** Rectángulo de una franja en píxeles de pantalla. */
data class StripRect(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

/**
 * Lo que decide dónde van las franjas. [imeTop] es el borde superior del teclado si está abierto;
 * [suspended] las desactiva todas (pantalla de bloqueo).
 */
data class EdgeLayoutInput(
    val screenWidth: Int,
    val screenHeight: Int,
    val sideThicknessPx: Int,
    val bottomThicknessPx: Int,
    /** Alto de la barra de estado: las franjas laterales empiezan debajo para no robar la cortina. */
    val topExclusionPx: Int,
    val imeTop: Int? = null,
    val suspended: Boolean = false,
    val enabled: Set<Edge> = Edge.entries.toSet(),
)

/**
 * Dónde va cada franja; `null` = no recibe toques. Las laterales van de la barra de estado al
 * borde de arriba de la inferior (o del teclado); la inferior ocupa todo el ancho y desaparece con
 * el teclado abierto, para no tapar su última fila (plan.md §5.9).
 */
fun layoutStrips(input: EdgeLayoutInput): Map<Edge, StripRect?> {
    val sideBottom = input.imeTop ?: (input.screenHeight - input.bottomThicknessPx)
    val sideHeight = sideBottom - input.topExclusionPx

    fun side(left: Int) =
        StripRect(left, input.topExclusionPx, input.sideThicknessPx, sideHeight).takeIf { sideHeight >= MIN_SIDE_HEIGHT_PX }
    val strips =
        mapOf(
            Edge.Left to side(0),
            Edge.Right to side(input.screenWidth - input.sideThicknessPx),
            Edge.Bottom to
                StripRect(0, input.screenHeight - input.bottomThicknessPx, input.screenWidth, input.bottomThicknessPx)
                    .takeIf { input.imeTop == null },
        )
    return strips.mapValues { (edge, rect) -> rect.takeIf { !input.suspended && edge in input.enabled } }
}

/** Una franja lateral más corta que esto (teclado enorme, apaisado) ya no se puede usar. */
private const val MIN_SIDE_HEIGHT_PX = 48
