package com.wh01s17.launcher.core.system.navigation

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.wh01s17.launcher.core.system.oem.xiaomi.RateLimiter
import com.wh01s17.launcher.core.system.oem.xiaomi.SecureSettingsGateway

/**
 * Vuelve a encender el servicio de accesibilidad si murió (plan.md §5.9, §11.1).
 *
 * Cuando su proceso muere, Android lo marca como *crashed* y no lo vuelve a enlazar; MIUI, además,
 * rechaza reiniciarlo sin *Inicio automático* (`AutoStartManagerService: Reject RestartService`,
 * comprobado en el Redmi). Los botones ya volvieron gracias a la vigilancia, pero los gestos
 * propios quedarían apagados hasta que el usuario toque el ajuste.
 *
 * Con `WRITE_SECURE_SETTINGS` se hace lo mismo que el usuario en Ajustes: quitar el servicio de
 * `enabled_accessibility_services` y volver a ponerlo. Sólo si **sigue en la lista** (si el usuario
 * lo quitó, se respeta) y como mucho [MAX_REVIVALS] veces cada 10 minutos, para no entrar en bucle
 * si el servicio fallara al arrancar.
 */
internal class AccessibilityReviver(
    private val context: Context,
    private val settings: SecureSettingsGateway,
    clock: () -> Long = System::currentTimeMillis,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val limiter = RateLimiter(MAX_REVIVALS, WINDOW_MILLIS, clock)
    private val component = ComponentName(context, WhNavigationService::class.java)

    /** `true` si inició una reactivación (el servicio volverá a conectarse en ~1 s). */
    fun reviveIfEnabled(): Boolean {
        val resolver = context.contentResolver
        val enabled = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val without = EnabledServices.without(enabled, component.packageName, component.className)
        if (without == null || !settings.canWrite() || !limiter.tryAcquire()) return false
        return try {
            Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, without)
            // Un momento después, de vuelta: Android lo enlaza de cero, como al activarlo a mano.
            handler.postDelayed(::restore, REENABLE_DELAY_MILLIS)
            Log.w(TAG, "reviving accessibility service")
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "cannot revive accessibility service", e)
            false
        }
    }

    private fun restore() {
        val resolver = context.contentResolver
        val current = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        try {
            Settings.Secure.putString(
                resolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                EnabledServices.with(current, component.flattenToString()),
            )
            Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
        } catch (e: SecurityException) {
            Log.w(TAG, "cannot re-enable accessibility service", e)
        }
    }

    private companion object {
        const val TAG = "WhNavigationWatchdog"
        const val MAX_REVIVALS = 3
        const val WINDOW_MILLIS = 10 * 60_000L
        const val REENABLE_DELAY_MILLIS = 500L
    }
}

/**
 * La lista `enabled_accessibility_services`: componentes separados por `:`, en forma larga
 * (`paquete/paquete.Clase`) o corta (`paquete/.Clase`). Se edita sin tocar los de otras apps.
 */
internal object EnabledServices {
    /** La lista sin nuestro servicio; `null` si no estaba (el usuario lo desactivó: no se revive). */
    fun without(
        value: String?,
        packageName: String,
        className: String,
    ): String? {
        val entries = split(value)
        val kept = entries.filterNot { matches(it, packageName, className) }
        return if (kept.size == entries.size) null else kept.joinToString(SEPARATOR)
    }

    fun contains(
        value: String?,
        packageName: String,
        className: String,
    ): Boolean = split(value).any { matches(it, packageName, className) }

    /** La lista con [component] añadido al final si no estaba. */
    fun with(
        value: String?,
        component: String,
    ): String {
        val entries = split(value)
        val (packageName, className) = component.split('/', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        return if (entries.any {
                matches(it, packageName, className)
            }
        ) {
            entries.joinToString(SEPARATOR)
        } else {
            (entries + component).joinToString(SEPARATOR)
        }
    }

    private fun split(value: String?) =
        value
            .orEmpty()
            .split(SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "\"\"" }

    private fun matches(
        entry: String,
        packageName: String,
        className: String,
    ): Boolean {
        val slash = entry.indexOf('/')
        if (slash <= 0 || entry.substring(0, slash) != packageName) return false
        val raw = entry.substring(slash + 1)
        return (if (raw.startsWith('.')) packageName + raw else raw) == className
    }

    private const val SEPARATOR = ":"
}
