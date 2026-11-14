package com.wh01s17.launcher.feature.widgets.host

import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * «Mantener pulsado» de un widget (plan.md §7 Hito 7): un toque corto nunca entra en edición. Antes,
 * un toque en el margen del widget (sin botón debajo) no veía su UP y a los 600 ms saltaba el modo
 * edición con su vibración (visto en el Redmi el 2026-11-14).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WhWidgetHostViewTest {
    private var longPresses = 0

    /** En una ventana de verdad: fuera de ella, `postDelayed` no corre y la prueba no probaría nada. */
    private fun host(withButton: Boolean): WhWidgetHostView {
        val controller = Robolectric.buildActivity(android.app.Activity::class.java).create()
        val activity = controller.get()
        val host = WhWidgetHostView(activity).apply { onLongPress = { longPresses++ } }
        if (withButton) {
            host.addView(View(activity).apply { isClickable = true }, FrameLayout.LayoutParams(100, 100))
        }
        activity.setContentView(host, FrameLayout.LayoutParams(100, 100))
        controller.start().resume().visible()
        advance(0)
        val exactly = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY)
        host.measure(exactly, exactly)
        host.layout(0, 0, 100, 100)
        check(host.isAttachedToWindow) { "the host view must be in a window" }
        return host
    }

    private fun WhWidgetHostView.send(action: Int): Boolean {
        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, action, 50f, 50f, 0)
        return dispatchTouchEvent(event).also { event.recycle() }
    }

    private fun advance(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    @Test
    fun `a quick tap on an empty part of the widget does not enter edit mode`() {
        val host = host(withButton = false)
        host.send(MotionEvent.ACTION_DOWN)
        // Sin botón debajo, Android no entrega el UP a esta vista: el temporizador no debe quedar vivo.
        advance(1_000)
        assertEquals(0, longPresses)
    }

    @Test
    fun `a quick tap on a button does not enter edit mode`() {
        val host = host(withButton = true)
        host.send(MotionEvent.ACTION_DOWN)
        advance(100)
        host.send(MotionEvent.ACTION_UP)
        advance(1_000)
        assertEquals(0, longPresses)
    }

    @Test
    fun `holding a button still enters edit mode`() {
        val host = host(withButton = true)
        host.send(MotionEvent.ACTION_DOWN)
        advance(700)
        assertEquals(1, longPresses)
    }
}
