package com.wh01s17.launcher.core.apps.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.ColorInt
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

/**
 * Pasa el `Drawable` de una app a un `Bitmap` cuadrado con el estilo de [IconStyle].
 *
 * - **Monocromo:** la capa `monochrome` del icono adaptativo (Android 13+), teñida. El icono
 *   adaptativo mide 108 unidades y sólo se ven las 72 centrales, así que la capa se dibuja a 1,5×
 *   y centrada, sin máscara: queda el glifo plano, como los iconos del shell.
 * - **Sin capa monocroma:** el original con la saturación a [fallbackSaturation] (plan.md §4.7).
 * - **Pixelado:** el original reducido a [PIXEL_GRID]×[PIXEL_GRID] (la rejilla de los iconos
 *   pixel), con el alfa a 0 o 255 y ampliado sin suavizado.
 */
class IconRenderer(
    private val fallbackSaturation: Float = FALLBACK_SATURATION,
) {
    fun render(
        icon: Drawable,
        style: IconStyle,
        sizePx: Int,
        @ColorInt tint: Int,
    ): Bitmap {
        require(sizePx > 0) { "sizePx must be positive: $sizePx" }
        return when (style) {
            IconStyle.Monochrome ->
                monochromeLayer(icon)?.let { drawMonochrome(it, sizePx, tint) }
                    ?: draw(icon, sizePx, desaturate(fallbackSaturation))
            IconStyle.Original -> draw(icon, sizePx, filter = null)
            IconStyle.Pixelated -> pixelate(icon, sizePx)
        }
    }

    private fun monochromeLayer(icon: Drawable): Drawable? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && icon is AdaptiveIconDrawable) icon.monochrome else null

    private fun drawMonochrome(
        layer: Drawable,
        sizePx: Int,
        @ColorInt tint: Int,
    ): Bitmap {
        val bitmap = createBitmap(sizePx, sizePx)
        val inset = (sizePx * ADAPTIVE_HIDDEN_FRACTION).toInt()
        layer.mutate().apply {
            colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN)
            setBounds(-inset, -inset, sizePx + inset, sizePx + inset)
            draw(Canvas(bitmap))
        }
        return bitmap
    }

    private fun draw(
        icon: Drawable,
        sizePx: Int,
        filter: ColorFilter?,
    ): Bitmap {
        val bitmap = createBitmap(sizePx, sizePx)
        icon.mutate().apply {
            if (filter != null) colorFilter = filter
            setBounds(0, 0, sizePx, sizePx)
            draw(Canvas(bitmap))
        }
        return bitmap
    }

    private fun pixelate(
        icon: Drawable,
        sizePx: Int,
    ): Bitmap {
        // Dibujar al doble y reducir a la mitad promedia mejor que reducir de golpe.
        val small = draw(icon, PIXEL_GRID * 2, filter = null).scale(PIXEL_GRID, PIXEL_GRID, filter = true)
        val pixels = IntArray(PIXEL_GRID * PIXEL_GRID)
        small.getPixels(pixels, 0, PIXEL_GRID, 0, 0, PIXEL_GRID, PIXEL_GRID)
        for (i in pixels.indices) pixels[i] = hardenAlpha(pixels[i])
        small.setPixels(pixels, 0, PIXEL_GRID, 0, 0, PIXEL_GRID, PIXEL_GRID)
        return small.scale(sizePx, sizePx, filter = false)
    }

    companion object {
        /** Lado de la rejilla de los iconos pixel del tema (Pixelarticons). */
        const val PIXEL_GRID = 24

        /** Saturación del icono original cuando no hay monocromo: color apagado, aún reconocible. */
        const val FALLBACK_SATURATION = 0.3f

        /** (108 − 72) / 2 / 72: lo que sobresale por cada lado la capa de un icono adaptativo. */
        private const val ADAPTIVE_HIDDEN_FRACTION = 0.25f

        private const val ALPHA_THRESHOLD = 128
        private const val ALPHA_SHIFT = 24
        private const val RGB_MASK = 0x00FFFFFF

        /** Píxel opaco o transparente, sin bordes suavizados. `getPixels` da colores sin premultiplicar. */
        internal fun hardenAlpha(argb: Int): Int {
            val alpha = argb ushr ALPHA_SHIFT
            return if (alpha < ALPHA_THRESHOLD) 0 else argb or RGB_MASK.inv()
        }

        private fun desaturate(saturation: Float) = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(saturation) })
    }
}
