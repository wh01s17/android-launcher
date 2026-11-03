package com.wh01s17.launcher.core.system.navigation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams

/**
 * Las franjas de gestos: una ventana `TYPE_ACCESSIBILITY_OVERLAY` transparente por borde. Sólo
 * el servicio de accesibilidad puede crearlas, y desaparecen solas si el servicio muere.
 *
 * Una franja sin rectángulo no se quita: se vuelve `FLAG_NOT_TOUCHABLE`, de modo que los toques
 * pasan a la app de debajo, y recuperarla no exige volver a crear la ventana.
 */
internal class EdgeStripOverlay(
    private val context: Context,
    private val windowManager: WindowManager,
    private val configFor: (Edge) -> EdgeGestureConfig,
    private val onAction: (NavAction) -> Unit,
) {
    private val views = mutableMapOf<Edge, EdgeStripView>()

    val attached: Boolean get() = views.isNotEmpty()

    /** `false` si el sistema no deja añadir las ventanas (p. ej. el servicio se está cerrando). */
    fun attach(layout: Map<Edge, StripRect?>): Boolean {
        if (attached) return true
        return try {
            for (edge in Edge.entries) {
                val view = EdgeStripView(context, EdgeGestureRecognizer(edge, configFor(edge)), onAction)
                windowManager.addView(view, paramsFor(edge, layout[edge]))
                views[edge] = view
            }
            true
        } catch (e: WindowManager.BadTokenException) {
            failed(e)
        } catch (e: WindowManager.InvalidDisplayException) {
            failed(e)
        } catch (e: IllegalStateException) {
            failed(e)
        }
    }

    private fun failed(error: RuntimeException): Boolean {
        Log.w(TAG, "cannot add edge strips", error)
        detach()
        return false
    }

    fun update(layout: Map<Edge, StripRect?>) {
        for ((edge, view) in views) {
            if (!view.isAttachedToWindow) continue
            windowManager.updateViewLayout(view, paramsFor(edge, layout[edge]))
        }
    }

    fun detach() {
        for (view in views.values) {
            view.cancelGesture()
            if (view.isAttachedToWindow) windowManager.removeViewImmediate(view)
        }
        views.clear()
    }

    private companion object {
        const val TAG = "WhNavigation"
    }

    private fun paramsFor(
        edge: Edge,
        rect: StripRect?,
    ): LayoutParams {
        // Sin rectángulo: ventana mínima que no recibe toques.
        val r = rect ?: StripRect(0, 0, 1, 1)
        val flags =
            LayoutParams.FLAG_NOT_FOCUSABLE or
                LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                (if (rect == null) LayoutParams.FLAG_NOT_TOUCHABLE else 0)
        return LayoutParams(r.width, r.height, LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, flags, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = r.left
            y = r.top
            title = "wh01s17-nav-${edge.name.lowercase()}"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Llegar al borde real de la pantalla, por encima de las barras del sistema.
                fitInsetsTypes = 0
                layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
    }
}

/**
 * Superficie táctil de una franja. No dibuja nada: el gesto se ve por la respuesta háptica y por
 * la acción. Se usan coordenadas de pantalla (`rawX`/`rawY`) para que el recorrido no dependa de
 * lo estrecha que es la ventana.
 */
@SuppressLint("ViewConstructor")
internal class EdgeStripView(
    context: Context,
    private val recognizer: EdgeGestureRecognizer,
    private val onAction: (NavAction) -> Unit,
) : View(context) {
    private val holdCheck =
        Runnable {
            if (recognizer.checkHold(SystemClock.uptimeMillis()) == GestureFeedback.RecentsArmed) tick(GestureFeedback.RecentsArmed)
        }

    // No es un control que se pulse: es una superficie de gestos, sin clic que anunciar.
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val time = event.eventTime
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> recognizer.down(event.rawX, event.rawY, time)
            MotionEvent.ACTION_MOVE -> recognizer.move(event.rawX, event.rawY, time)?.let(::tick)
            MotionEvent.ACTION_UP -> recognizer.up(event.rawX, event.rawY, time)?.let(onAction)
            // Un segundo dedo o la cancelación del sistema anulan el gesto.
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> recognizer.cancel()
        }
        scheduleHoldCheck()
        return true
    }

    fun cancelGesture() {
        removeCallbacks(holdCheck)
        recognizer.cancel()
    }

    private fun scheduleHoldCheck() {
        removeCallbacks(holdCheck)
        val deadline = recognizer.holdDeadline ?: return
        postDelayed(holdCheck, (deadline - SystemClock.uptimeMillis()).coerceAtLeast(0))
    }

    private fun tick(feedback: GestureFeedback) {
        val constant =
            when {
                feedback == GestureFeedback.Disarmed -> return
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
                else -> HapticFeedbackConstants.VIRTUAL_KEY
            }
        performHapticFeedback(constant)
    }
}
