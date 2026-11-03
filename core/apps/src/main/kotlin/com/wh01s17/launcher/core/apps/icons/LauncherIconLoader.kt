package com.wh01s17.launcher.core.apps.icons

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import androidx.core.graphics.drawable.toDrawable
import com.wh01s17.launcher.core.apps.source.AndroidLauncherAppsGateway
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * [AppIconLoader] sobre `LauncherApps`: resuelve la actividad, pide su icono a la densidad de la
 * pantalla, lo pasa por [IconRenderer] y lo guarda en [cache]. Las apps de otros perfiles llevan
 * la insignia del sistema (el maletín del perfil de trabajo).
 */
class LauncherIconLoader(
    context: Context,
    private val gateway: AndroidLauncherAppsGateway,
    private val ioDispatcher: CoroutineDispatcher,
    private val renderer: IconRenderer = IconRenderer(),
    private val cache: IconCache = IconCache(),
) : AppIconLoader {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val packageManager = appContext.packageManager
    private val densityDpi get() = appContext.resources.displayMetrics.densityDpi

    override fun cached(request: IconRequest): Bitmap? = cache[request]

    override suspend fun load(request: IconRequest): Bitmap? =
        cache[request] ?: withContext(ioDispatcher) {
            render(request)?.also { cache.put(request, it) }
        }

    private fun render(request: IconRequest): Bitmap? {
        val user = gateway.userHandle(request.key.userSerial) ?: return null
        return resolveIcon(request, user)?.let { icon ->
            val styled = renderer.render(icon, request.style, request.sizePx, request.tint)
            if (user == Process.myUserHandle()) styled else badge(styled, user, request)
        }
    }

    /** `null` si la app se desinstaló o el perfil desapareció desde que se pidió el icono. */
    private fun resolveIcon(
        request: IconRequest,
        user: UserHandle,
    ): Drawable? {
        val component = ComponentName(request.key.packageName, request.key.className)
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(component)
        return runCatching { launcherApps.resolveActivity(intent, user) }.getOrNull()?.getIcon(densityDpi)
    }

    private fun badge(
        styled: Bitmap,
        user: UserHandle,
        request: IconRequest,
    ): Bitmap {
        val badged = packageManager.getUserBadgedIcon(styled.toDrawable(appContext.resources), user)
        return renderer.render(badged, IconStyle.Original, request.sizePx, request.tint)
    }
}
