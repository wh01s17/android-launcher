package com.wh01s17.launcher.core.apps.icons

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import com.wh01s17.launcher.core.apps.model.AppEntry
import com.wh01s17.launcher.core.apps.model.AppKey

/** Estilo de los iconos de apps (plan.md §4.7, `apps.iconStyle` de la config). */
enum class IconStyle(
    val configName: String,
) {
    /** Capa monocroma del icono adaptativo teñida; sin ella, el original desaturado. */
    Monochrome("monochrome"),

    /** El icono de la app tal cual. */
    Original("original"),

    /** El icono reducido a la rejilla de 24 de los iconos pixel del tema. */
    Pixelated("pixelated"),
    ;

    companion object {
        val Default = Monochrome

        /** Valor de `launcher.json`; uno desconocido vuelve al de por defecto. */
        fun fromConfig(value: String?): IconStyle = entries.firstOrNull { it.configName == value } ?: Default
    }
}

/**
 * Un icono concreto. Es también la clave de la caché: [iconStamp] cambia al actualizarse la app,
 * así que el icono viejo deja de pedirse y el LRU lo descarta.
 */
data class IconRequest(
    val key: AppKey,
    val iconStamp: Long,
    val style: IconStyle,
    val sizePx: Int,
    /** Color del monocromo: lo da quien pide el icono, desde los tokens del tema (R6). */
    @param:ColorInt val tint: Int,
) {
    constructor(
        entry: AppEntry,
        style: IconStyle,
        sizePx: Int,
        @ColorInt tint: Int,
    ) :
        this(entry.key, entry.iconStamp, style, sizePx, tint)
}

/** Carga iconos de apps fuera del hilo principal y los guarda en memoria. */
interface AppIconLoader {
    /** El icono si ya está en memoria; para pintar el primer fotograma sin esperar. */
    fun cached(request: IconRequest): Bitmap?

    /** El icono, de la caché o generado; `null` si la app ya no existe. */
    suspend fun load(request: IconRequest): Bitmap?
}
