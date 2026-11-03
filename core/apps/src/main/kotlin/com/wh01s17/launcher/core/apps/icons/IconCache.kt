package com.wh01s17.launcher.core.apps.icons

import android.graphics.Bitmap
import android.util.LruCache

/**
 * Caché LRU de iconos acotada en bytes (plan.md §7, Hito 11: "caché de iconos acotada").
 *
 * Por defecto usa 1/8 del heap de la app: con 156 apps a 144 px (48 dp a 480 dpi) los iconos de
 * un estilo ocupan unos 13 MB, que caben de sobra en el heap de un launcher.
 */
class IconCache(
    maxBytes: Int = defaultMaxBytes(),
) {
    private val lru =
        object : LruCache<IconRequest, Bitmap>(maxBytes) {
            override fun sizeOf(
                key: IconRequest,
                value: Bitmap,
            ): Int = value.allocationByteCount
        }

    operator fun get(request: IconRequest): Bitmap? = lru.get(request)

    fun put(
        request: IconRequest,
        bitmap: Bitmap,
    ) {
        lru.put(request, bitmap)
    }

    /** Ocupación actual, para pruebas y diagnóstico. */
    val sizeBytes: Int get() = lru.size()

    fun clear() = lru.evictAll()

    private companion object {
        const val HEAP_FRACTION = 8

        fun defaultMaxBytes(): Int = (Runtime.getRuntime().maxMemory() / HEAP_FRACTION).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
