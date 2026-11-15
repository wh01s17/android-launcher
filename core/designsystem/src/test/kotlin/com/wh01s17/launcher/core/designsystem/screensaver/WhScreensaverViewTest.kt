package com.wh01s17.launcher.core.designsystem.screensaver

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.wh01s17.launcher.core.designsystem.palette.TidalampPalettes
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Cada estilo se dibuja sobre negro puro y enciende algo; lo de alrededor queda apagado (OLED). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class WhScreensaverViewTest {
    private fun draw(settings: ScreensaverSettings): Bitmap {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = WhScreensaverView(context, flowOf(TidalampPalettes.default), flowOf(settings))
        val spec = View.MeasureSpec.makeMeasureSpec(SIZE, View.MeasureSpec.EXACTLY)
        view.measure(spec, spec)
        view.layout(0, 0, SIZE, SIZE)
        // Sin ventana no recoge el flujo: los ajustes se aplican a mano.
        view.applySettings(settings)
        return Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
    }

    private fun lit(bitmap: Bitmap): Int {
        val pixels = IntArray(SIZE * SIZE).also { bitmap.getPixels(it, 0, SIZE, 0, 0, SIZE, SIZE) }
        return pixels.count { it and RGB != 0 }
    }

    @Test
    fun `every style draws something on pure black and leaves the corners off`() {
        val lines = ScreensaverSettings(showTime = true, showDate = true, showText = true)
        for (style in listOf(ScreensaverStyle.Clock, ScreensaverStyle.Text, ScreensaverStyle.Minimal)) {
            val bitmap = draw(lines.copy(style = style))
            assertTrue("$style lit nothing", lit(bitmap) > 0)
            assertEquals("$style corner", BLACK, bitmap.getPixel(0, 0))
            assertEquals("$style corner", BLACK, bitmap.getPixel(SIZE - 1, SIZE - 1))
        }
    }

    @Test
    fun `lower brightness lights fewer full pixels`() {
        val bright = draw(ScreensaverSettings(style = ScreensaverStyle.Text, brightness = 1f))
        val dim = draw(ScreensaverSettings(style = ScreensaverStyle.Text, brightness = 0.2f))

        fun maxGreen(bitmap: Bitmap) =
            IntArray(SIZE * SIZE).also { bitmap.getPixels(it, 0, SIZE, 0, 0, SIZE, SIZE) }.maxOf { (it shr GREEN_SHIFT) and BYTE }
        assertTrue(maxGreen(dim) < maxGreen(bright))
    }

    @Test
    fun `spinning text turns edge-on at a quarter turn`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val painter = ScreensaverPainter(context)
        val settings = ScreensaverSettings(style = ScreensaverStyle.Text, text = "hack the planet", spinText = true)

        fun render(yaw: Double) =
            Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).also {
                painter.draw(Canvas(it), SIZE, SIZE, settings, TidalampPalettes.default, IntArray(0), null, 0L, yaw)
            }
        val facing = lit(render(0.0))
        val turned = lit(render(Math.toRadians(TURNED_DEGREES)))
        val edgeOn = lit(render(Math.PI / 2))
        // Girado, más estrecho; de canto, nada (antes se dibujaba entero: un parpadeo en cada vuelta).
        assertTrue("facing=$facing turned=$turned", turned < facing)
        assertTrue("facing=$facing edgeOn=$edgeOn", edgeOn * EDGE_RATIO < facing)
    }

    private companion object {
        const val SIZE = 400
        const val RGB = 0xFFFFFF
        const val BLACK = -0x1000000
        const val GREEN_SHIFT = 8
        const val BYTE = 0xFF

        /** De canto, el texto enciende mucho menos que de frente (el cursor parpadeante aparte). */
        const val EDGE_RATIO = 4
        const val TURNED_DEGREES = 75.0
    }
}
