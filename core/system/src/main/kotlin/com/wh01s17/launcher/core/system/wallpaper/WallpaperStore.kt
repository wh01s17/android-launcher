package com.wh01s17.launcher.core.system.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AtomicFile
import com.wh01s17.launcher.core.system.R
import java.io.File
import java.io.IOException

/** Las imágenes importadas se copian, se acotan y se decodifican antes de reemplazar la anterior de forma atómica. */
class WallpaperStore(
    context: Context,
) {
    private val appContext = context.applicationContext
    val revision = kotlinx.coroutines.flow.MutableStateFlow(0)
    private val imported = File(appContext.filesDir, "wallpaper.png")

    fun load(id: String): Bitmap? =
        when (id) {
            "none" -> null
            "imported" -> imported.takeIf { it.isFile }?.let { BitmapFactory.decodeFile(it.path) }
            in BUILT_INS -> appContext.assets.open("wallpapers/${builtInSize()}/$id.png").use { BitmapFactory.decodeStream(it) }
            else -> null
        }

    /** La carpeta de fondos incluidos más cercana al ancho de la pantalla (en vertical). */
    private fun builtInSize(): String {
        val metrics = appContext.resources.displayMetrics
        return nearestSize(minOf(metrics.widthPixels, metrics.heightPixels))
    }

    @Synchronized
    fun importImage(uri: Uri) {
        val bitmap = decodeImage(uri)
        try {
            writeImage(bitmap)
            revision.value += 1
        } finally {
            bitmap.recycle()
        }
    }

    private fun readBounds(uri: Uri): BitmapFactory.Options {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val header =
            appContext.contentResolver.openInputStream(uri) ?: throw IOException(appContext.getString(R.string.wallpaper_error_read))
        header.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException(appContext.getString(R.string.wallpaper_error_format))
        return bounds
    }

    private fun decodeImage(uri: Uri): Bitmap {
        val bounds = readBounds(uri)
        var sample = 1
        val edge = maxOf(bounds.outWidth, bounds.outHeight).toLong()
        while ((edge + sample - 1) / sample > MAX_EDGE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return appContext.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException(appContext.getString(R.string.wallpaper_error_decode))
    }

    private fun writeImage(bitmap: Bitmap) {
        val atomic = AtomicFile(imported)
        val stream = atomic.startWrite()
        try {
            if (!bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    PNG_QUALITY,
                    stream,
                )
            ) {
                throw IOException(appContext.getString(R.string.wallpaper_error_save))
            }
            atomic.finishWrite(stream)
        } catch (error: IOException) {
            atomic.failWrite(stream)
            throw error
        }
    }

    fun setSystem(id: String) {
        val bitmap = load(id) ?: throw IOException(appContext.getString(R.string.wallpaper_error_none))
        try {
            WallpaperManager.getInstance(appContext).setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
        } finally {
            bitmap.recycle()
        }
    }

    companion object {
        private val BUILT_INS = setOf("mark", "moon", "moon-wordmark")

        /** Tamaños generados por `tools/render-wallpapers.py` (plan.md §4.7). */
        private val SIZES = listOf(1080 to "1080x2400", 1220 to "1220x2712", 1440 to "1440x3200")

        /** El tamaño incluido cuyo ancho se acerca más a [width]. */
        internal fun nearestSize(width: Int): String = SIZES.minBy { (w, _) -> kotlin.math.abs(w - width) }.second

        private const val PNG_QUALITY = 100
        private const val MAX_EDGE = 2048
    }
}
