package com.wh01s17.launcher.core.system.navigation

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import com.wh01s17.launcher.core.system.oem.xiaomi.XiaomiGestureCompat

/**
 * Vigilancia desde el proceso del launcher para no dejar el teléfono sin navegación (plan.md §5.9):
 *
 * - Con el modo *gestos wh01s17* activo se enlaza a [NavigationLinkService], que vive en el
 *   proceso de navegación. Si ese proceso muere (el sistema lo mata, falla), Android avisa al
 *   instante con `onServiceDisconnected` y se devuelven los botones; [WhNavigationService] no
 *   puede hacerlo porque ya no existe.
 * - [check] se llama en cada `onResume` del launcher: si el servicio de accesibilidad no está
 *   activo, también se devuelven los botones.
 * - En los dos casos, si el servicio sigue activado en Ajustes pero murió, [AccessibilityReviver]
 *   lo vuelve a encender (Android y MIUI no lo reinician solos).
 *
 * Lo que no puede cubrir: que se detengan a la vez los dos procesos (forzar detención) con otra
 * app delante. Para eso está la recuperación documentada (`docs/xiaomi.md`) y el `onResume` del
 * siguiente arranque.
 */
class NavigationWatchdog(
    context: Context,
    private val compat: XiaomiGestureCompat = XiaomiGestureCompat(context),
) {
    private val appContext = context.applicationContext
    private val reviver = AccessibilityReviver(appContext, compat.settings)
    private val handler = Handler(Looper.getMainLooper())
    private var bound = false

    /** `true` mientras hay enlace con el proceso de navegación: su muerte se notará al instante. */
    @Volatile
    var linked = false
        private set

    /** Momento de la última vez que hubo que devolver los botones (para avisar en la UI). */
    var lastRestoreAtMillis: Long? = null
        private set

    private val connection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName,
                service: IBinder,
            ) {
                linked = true
                Log.i(TAG, "linked to navigation process")
            }

            // El proceso de navegación murió: la capa 3 ya no existe.
            override fun onServiceDisconnected(name: ComponentName) {
                linked = false
                secure("navigation process died")
            }

            // El enlace murió sin que muriera el proceso (p. ej. MIUI rechazó arrancarlo): se comprueba
            // si la navegación sigue viva y se vuelve a enlazar con calma, sin bucles.
            override fun onBindingDied(name: ComponentName) {
                linked = false
                unbind()
                restoreIfNeeded(isNavigationServiceRunning(), "navigation binding died")
                handler.removeCallbacks(rebind)
                handler.postDelayed(rebind, REBIND_DELAY_MILLIS)
            }
        }

    /** Al arrancar el proceso del launcher. */
    fun start() = bindIfEnabled()

    /**
     * Desde `onResume`: `true` si encontró los botones ocultos sin capa 3 y los devolvió. También
     * rehace el enlace con el proceso de navegación si el modo está activo y aún no lo hay.
     */
    fun check(): Boolean {
        bindIfEnabled()
        val running = isNavigationServiceRunning()
        val restored = restoreIfNeeded(running, "service not running on resume")
        // Recién activado, el servicio tarda en enlazarse: sólo se revive si sigue sin estar.
        handler.removeCallbacks(reviveIfStillDown)
        if (!running) handler.postDelayed(reviveIfStillDown, REVIVE_GRACE_MILLIS)
        return restored
    }

    private val rebind = Runnable { bindIfEnabled() }

    private val reviveIfStillDown =
        Runnable {
            if (!isNavigationServiceRunning()) reviver.reviveIfEnabled()
        }

    /** El usuario activa o desactiva el modo en el asistente. */
    fun setModeEnabled(enabled: Boolean) {
        val prefs = compat.files.readPrefs()
        if (enabled) {
            compat.files.writePrefs(compat.controller.enable(prefs))
            bindIfEnabled()
        } else {
            // Guardar antes de escribir: el proceso de navegación no debe volver a ocultarlos.
            compat.files.writePrefs(compat.controller.disable(prefs))
            unbind()
            compat.controller.restoreAfterDisable(prefs)
        }
        appContext.sendBroadcast(Intent(XiaomiGestureCompat.modeChangedAction(appContext)).setPackage(appContext.packageName))
    }

    /** `true` si el usuario tiene activado el servicio en Ajustes → Accesibilidad (vivo o no). */
    fun isNavigationServiceEnabled(): Boolean {
        val value = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return EnabledServices.contains(value, appContext.packageName, WhNavigationService::class.java.name)
    }

    /** `true` si Android tiene conectado nuestro servicio de accesibilidad ahora mismo. */
    fun isNavigationServiceRunning(): Boolean {
        val manager = appContext.getSystemService(AccessibilityManager::class.java) ?: return false
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any { info ->
            val service = info.resolveInfo?.serviceInfo ?: return@any false
            service.packageName == appContext.packageName && service.name == WhNavigationService::class.java.name
        }
    }

    private fun secure(reason: String) {
        Log.i(TAG, "watchdog: $reason")
        restoreIfNeeded(navigationAlive = false, reason)
        reviver.reviveIfEnabled()
    }

    private fun restoreIfNeeded(
        navigationAlive: Boolean,
        reason: String,
    ): Boolean {
        val restored = compat.controller.ensureSafe(compat.files.readPrefs(), navigationAlive)
        if (restored) {
            lastRestoreAtMillis = System.currentTimeMillis()
            Log.w(TAG, "buttons restored: $reason")
        }
        return restored
    }

    private fun bindIfEnabled() {
        if (bound || !compat.files.readPrefs().enabled) return
        bound = appContext.bindService(Intent(appContext, NavigationLinkService::class.java), connection, Context.BIND_AUTO_CREATE)
        if (!bound) {
            // Sin enlace no hay aviso inmediato: se reintenta mientras el modo siga activo.
            handler.removeCallbacks(rebind)
            handler.postDelayed(rebind, REBIND_DELAY_MILLIS)
        }
    }

    private fun unbind() {
        linked = false
        if (!bound) return
        appContext.unbindService(connection)
        bound = false
    }

    private companion object {
        const val TAG = "WhNavigationWatchdog"

        /** Un servicio recién activado se enlaza en ~1 s en el Redmi; con 3 s no hay falsos positivos. */
        const val REVIVE_GRACE_MILLIS = 3_000L
        const val REBIND_DELAY_MILLIS = 1_000L
    }
}

/**
 * Servicio vacío en el proceso de navegación: su único fin es que el launcher se entere en cuanto
 * ese proceso muere (el binder deja de existir y Android avisa a quien está enlazado).
 */
class NavigationLinkService : Service() {
    private val binder = Binder()

    override fun onBind(intent: Intent): IBinder = binder
}
