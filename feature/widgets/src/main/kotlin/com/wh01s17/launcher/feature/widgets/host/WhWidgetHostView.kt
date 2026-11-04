package com.wh01s17.launcher.feature.widgets.host

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.hypot

/**
 * La vista de un widget dentro de un tile. Como en Launcher3, vigila los toques antes que el widget
 * para reconocer «mantener pulsado» ([onLongPress], entrar en edición) aunque el widget tenga
 * botones o listas. Con [interactive] en `false` (modo edición) no se queda con ningún toque: así
 * el tile se puede arrastrar. Al cambiar de tamaño se lo dice al widget, en dp.
 */
class WhWidgetHostView(
    context: Context,
) : AppWidgetHostView(context) {
    var interactive: Boolean = true
    var onLongPress: () -> Unit = {}

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var longPressed = false
    private val longPress =
        Runnable {
            longPressed = true
            parent?.requestDisallowInterceptTouchEvent(true)
            onLongPress()
        }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean = interactive && super.dispatchTouchEvent(event)

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                longPressed = false
                postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> if (hypot(event.x - downX, event.y - downY) > slop) removeCallbacks(longPress)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> removeCallbacks(longPress)
        }
        // Tras la pulsación larga el gesto es del launcher: el widget recibe un CANCEL.
        return longPressed
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) longPressed = false
        return longPressed || super.onTouchEvent(event)
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int,
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        val density = resources.displayMetrics.density
        val width = w / density
        val height = h / density
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            updateAppWidgetSize(Bundle(), listOf(SizeF(width, height)))
        } else {
            @Suppress("DEPRECATION")
            updateAppWidgetSize(null, width.toInt(), height.toInt(), width.toInt(), height.toInt())
        }
    }
}
