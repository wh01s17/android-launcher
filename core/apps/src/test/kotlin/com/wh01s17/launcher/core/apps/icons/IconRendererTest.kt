package com.wh01s17.launcher.core.apps.icons

import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class IconRendererTest {
    private val renderer = IconRenderer()
    private val tint = Color.rgb(0xCB, 0xD5, 0xCE)

    private fun adaptive(monochrome: android.graphics.drawable.Drawable?) =
        AdaptiveIconDrawable(ColorDrawable(Color.BLUE), ColorDrawable(Color.YELLOW), monochrome)

    @Test
    fun `original keeps the colors`() {
        val bitmap = renderer.render(ColorDrawable(Color.RED), IconStyle.Original, 48, tint)
        assertEquals(48, bitmap.width)
        assertEquals(Color.RED, bitmap.getPixel(24, 24))
    }

    @Test
    fun `monochrome tints the layer and shows only the visible 72 of 108 units`() {
        // Cuadrado en el tercio central de la capa (36..72 de 108). Visible: 18..90 → a 72 px, 18..54.
        val layer = InsetDrawable(ColorDrawable(Color.BLACK), 1f / 3f)
        val bitmap = renderer.render(adaptive(layer), IconStyle.Monochrome, 72, tint)
        assertEquals(tint, bitmap.getPixel(36, 36))
        assertEquals(tint, bitmap.getPixel(19, 19))
        assertEquals(0, Color.alpha(bitmap.getPixel(16, 16)))
        assertEquals(0, Color.alpha(bitmap.getPixel(56, 56)))
        // Sin máscara ni fondo: el azul y el amarillo de las otras capas no aparecen.
        assertEquals(0, Color.alpha(bitmap.getPixel(2, 2)))
    }

    @Test
    fun `without a monochrome layer the original is desaturated`() {
        val bitmap = renderer.render(ColorDrawable(Color.RED), IconStyle.Monochrome, 48, tint)
        val pixel = bitmap.getPixel(24, 24)
        assertNotEquals(Color.RED, pixel)
        assertTrue("still reddish: ${Integer.toHexString(pixel)}", Color.red(pixel) > Color.green(pixel))
        assertTrue("less saturated: ${Integer.toHexString(pixel)}", Color.green(pixel) > 0)
        assertEquals(255, Color.alpha(pixel))
    }

    @Test
    fun `adaptive icon without monochrome also falls back`() {
        val bitmap = renderer.render(adaptive(null), IconStyle.Monochrome, 48, tint)
        assertNotEquals(tint, bitmap.getPixel(24, 24))
    }

    @Test
    fun `pixelated has hard alpha and 24 by 24 blocks`() {
        val circle = ShapeDrawable(OvalShape()).apply { paint.color = Color.GREEN }
        val size = IconRenderer.PIXEL_GRID * 6
        val bitmap = renderer.render(circle, IconStyle.Pixelated, size, tint)
        val pixels = IntArray(size * size).also { bitmap.getPixels(it, 0, size, 0, 0, size, size) }
        assertTrue(pixels.all { Color.alpha(it) == 0 || Color.alpha(it) == 255 })
        // Cada bloque de 6×6 px es un solo color.
        for (y in 0 until size) {
            for (x in 0 until size) {
                assertEquals("($x,$y)", pixels[(y / 6 * 6) * size + x / 6 * 6], pixels[y * size + x])
            }
        }
        assertEquals(255, Color.alpha(bitmap.getPixel(size / 2, size / 2)))
        assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
    }

    @Test
    fun `hardenAlpha rounds alpha to opaque or transparent`() {
        assertEquals(0, IconRenderer.hardenAlpha(Color.argb(127, 10, 20, 30)))
        assertEquals(Color.argb(255, 10, 20, 30), IconRenderer.hardenAlpha(Color.argb(128, 10, 20, 30)))
    }

    @Test
    fun `style names match the config`() {
        assertEquals(IconStyle.Pixelated, IconStyle.fromConfig("pixelated"))
        assertEquals(IconStyle.Monochrome, IconStyle.fromConfig("nope"))
        assertEquals(IconStyle.Monochrome, IconStyle.fromConfig(null))
    }
}
