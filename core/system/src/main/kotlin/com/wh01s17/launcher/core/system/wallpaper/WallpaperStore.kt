package com.wh01s17.launcher.core.system.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AtomicFile
import java.io.File
import java.io.IOException

/** Imported images are copied, bounded and decoded before atomically replacing the previous image. */
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
            in BUILT_INS -> appContext.assets.open("wallpapers/1220x2712/$id.png").use { BitmapFactory.decodeStream(it) }
            else -> null
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
        val header = appContext.contentResolver.openInputStream(uri) ?: throw IOException("No se pudo leer la imagen")
        header.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Formato de imagen no compatible")
        return bounds
    }

    private fun decodeImage(uri: Uri): Bitmap {
        val bounds = readBounds(uri)
        var sample = 1
        val edge = maxOf(bounds.outWidth, bounds.outHeight).toLong()
        while ((edge + sample - 1) / sample > MAX_EDGE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return appContext.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("No se pudo decodificar la imagen")
    }

    private fun writeImage(bitmap: Bitmap) {
        val atomic = AtomicFile(imported)
        val stream = atomic.startWrite()
        try {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, stream)) throw IOException("No se pudo guardar la imagen")
            atomic.finishWrite(stream)
        } catch (error: IOException) {
            atomic.failWrite(stream)
            throw error
        }
    }

    fun setSystem(id: String) {
        val bitmap = load(id) ?: throw IOException("Selecciona un fondo disponible primero")
        try {
            WallpaperManager.getInstance(appContext).setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
        } finally {
            bitmap.recycle()
        }
    }

    companion object {
        private val BUILT_INS = setOf("mark", "moon", "moon-wordmark")
        private const val PNG_QUALITY = 100
        private const val MAX_EDGE = 2048
    }
}
