package com.wh01s17.launcher.core.system.navigation

import kotlin.math.abs
import kotlin.math.hypot

/** Borde de la pantalla con una franja de gestos (plan.md §5.9, capa 3). */
enum class Edge { Left, Right, Bottom }

/** Acción global que dispara un gesto de borde. */
enum class NavAction { Back, Home, Recents }

/** Aviso para dar respuesta háptica mientras el dedo sigue en la pantalla. */
enum class GestureFeedback {
    /** Se cruzó la distancia: al soltar habrá acción. */
    Armed,

    /** Se volvió hacia el borde: al soltar no pasará nada. */
    Disarmed,

    /** Deslizar desde abajo y mantener: al soltar se abren los recientes. */
    RecentsArmed,
}

/**
 * Umbrales de un gesto, en píxeles. [triggerDistancePx] es cuánto hay que alejarse del borde;
 * [stillSlopPx] cuánto puede temblar el dedo sin dejar de contar como "quieto";
 * [recentsHoldMillis] cuánto hay que mantenerlo quieto para pedir recientes.
 */
data class EdgeGestureConfig(
    val triggerDistancePx: Float,
    val stillSlopPx: Float,
    val recentsHoldMillis: Long = DEFAULT_RECENTS_HOLD_MILLIS,
) {
    companion object {
        const val DEFAULT_RECENTS_HOLD_MILLIS = 300L
    }
}

/**
 * Reconocedor de un gesto que empieza en [edge]. Sin Android: recibe posiciones y tiempos y
 * decide, así que se prueba en la JVM.
 *
 * - Bordes laterales: alejarse del borde [EdgeGestureConfig.triggerDistancePx] con un ángulo de
 *   menos de 45° → *atrás* al soltar. Volver hacia el borde antes de soltar lo cancela, como en
 *   Android.
 * - Borde inferior: subir esa distancia → *inicio* al soltar; si además el dedo se queda quieto
 *   [EdgeGestureConfig.recentsHoldMillis] → *recientes*. Admite más inclinación (hasta ~63°): al
 *   subir el pulgar se desvía.
 *
 * La acción se decide al soltar (nunca a mitad del gesto), así que un toque accidental en la
 * franja no hace nada.
 */
class EdgeGestureRecognizer(
    val edge: Edge,
    private val config: EdgeGestureConfig,
) {
    private var tracking = false
    private var armed = false
    private var recentsArmed = false
    private var downX = 0f
    private var downY = 0f
    private var anchorX = 0f
    private var anchorY = 0f
    private var stillSince = 0L

    /** Cuándo se armarían los recientes si el dedo no se mueve; `null` si no aplica. */
    val holdDeadline: Long?
        get() = if (waitingForHold) stillSince + config.recentsHoldMillis else null

    /** Gesto inferior armado, aún sin recientes: sólo entonces cuenta mantener el dedo. */
    private val waitingForHold: Boolean
        get() = tracking && edge == Edge.Bottom && (armed && !recentsArmed)

    fun down(
        x: Float,
        y: Float,
        timeMillis: Long,
    ) {
        tracking = true
        armed = false
        recentsArmed = false
        downX = x
        downY = y
        anchorX = x
        anchorY = y
        stillSince = timeMillis
    }

    fun move(
        x: Float,
        y: Float,
        timeMillis: Long,
    ): GestureFeedback? {
        if (!tracking) return null
        if (hypot(x - anchorX, y - anchorY) > config.stillSlopPx) {
            anchorX = x
            anchorY = y
            stillSince = timeMillis
        }
        val past = isPastThreshold(x, y)
        return when {
            past && !armed -> GestureFeedback.Armed.also { armed = true }
            !past && armed -> GestureFeedback.Disarmed.also { disarm() }
            else -> checkHold(timeMillis)
        }
    }

    /** Se llama al vencer [holdDeadline]; devuelve [GestureFeedback.RecentsArmed] si acaba de armarlos. */
    fun checkHold(timeMillis: Long): GestureFeedback? {
        val deadline = holdDeadline ?: return null
        return if (timeMillis >= deadline) GestureFeedback.RecentsArmed.also { recentsArmed = true } else null
    }

    fun up(
        x: Float,
        y: Float,
        timeMillis: Long,
    ): NavAction? {
        if (!tracking) return null
        move(x, y, timeMillis)
        checkHold(timeMillis)
        val action =
            when {
                !armed -> null
                edge != Edge.Bottom -> NavAction.Back
                recentsArmed -> NavAction.Recents
                else -> NavAction.Home
            }
        cancel()
        return action
    }

    fun cancel() {
        tracking = false
        disarm()
    }

    private fun disarm() {
        armed = false
        recentsArmed = false
    }

    private fun isPastThreshold(
        x: Float,
        y: Float,
    ): Boolean {
        val dx = x - downX
        val dy = y - downY
        val (inward, across, slope) =
            when (edge) {
                Edge.Left -> Triple(dx, abs(dy), SIDE_MAX_SLOPE)
                Edge.Right -> Triple(-dx, abs(dy), SIDE_MAX_SLOPE)
                Edge.Bottom -> Triple(-dy, abs(dx), BOTTOM_MAX_SLOPE)
            }
        return inward >= config.triggerDistancePx && across < inward * slope
    }

    private companion object {
        /** tan 45°: un desplazamiento lateral que se tuerce más ya es un scroll vertical. */
        const val SIDE_MAX_SLOPE = 1f

        /** tan ~63°: al subir el pulgar se desvía hacia un lado. */
        const val BOTTOM_MAX_SLOPE = 2f
    }
}
