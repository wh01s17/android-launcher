package com.wh01s17.launcher.core.system.navigation

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.wh01s17.launcher.core.system.oem.xiaomi.GestureModePrefs
import com.wh01s17.launcher.core.system.oem.xiaomi.GestureModeStatus
import com.wh01s17.launcher.core.system.oem.xiaomi.NavigationStatus
import com.wh01s17.launcher.core.system.oem.xiaomi.SystemSetting
import com.wh01s17.launcher.core.system.oem.xiaomi.XiaomiGestureCompat

/**
 * Navegación propia por gestos de borde (plan.md §5.9, capa 3): *atrás* desde los lados, *inicio*
 * desde abajo y *recientes* subiendo y manteniendo, con `performGlobalAction`.
 *
 * También lleva la parte de la capa 2 que depende de estar vivo: oculta los botones (si el usuario
 * activó el modo *gestos wh01s17*) **sólo después** de poner las franjas, los vuelve a ocultar si
 * HyperOS los devuelve, y los restaura en [onUnbind]/[onDestroy] antes de irse.
 *
 * Corre en su propio proceso (`:nav`): si el launcher falla, la navegación sigue; si este proceso
 * muere, el launcher lo nota al instante ([NavigationWatchdog]) y devuelve los botones.
 *
 * Privacidad: pide ver las ventanas sólo para saber si el teclado está abierto y no taparlo. No lee
 * el contenido de ninguna app.
 */
class WhNavigationService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val geometry by lazy { NavigationGeometry(this) }
    private lateinit var compat: XiaomiGestureCompat
    private var overlay: EdgeStripOverlay? = null
    private var stopping = false

    private val forceFsgObserver =
        object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) = decide { compat.controller.onForceFsgChanged(it) }
        }

    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                if (intent.action == XiaomiGestureCompat.modeChangedAction(context)) {
                    decide { compat.controller.onNavigationReady(it) }
                } else {
                    refreshLayout()
                }
            }
        }

    override fun onServiceConnected() {
        super.onServiceConnected()
        compat = XiaomiGestureCompat(this)
        val strips = EdgeStripOverlay(this, getSystemService(WindowManager::class.java), geometry::configFor, ::perform)
        if (!strips.attach(layoutStrips(geometry.layoutInput(windows)))) {
            // Sin franjas no hay capa 3: los botones no se tocan.
            Log.w(TAG, "edge strips could not be added")
            return
        }
        overlay = strips
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(XiaomiGestureCompat.modeChangedAction(this@WhNavigationService))
            }
        // El aviso del modo viene del propio launcher (mismo UID): no hace falta exportar.
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        contentResolver.registerContentObserver(Settings.Global.getUriFor(SystemSetting.ForceFsgNavBar.key), false, forceFsgObserver)
        decide { compat.controller.onNavigationReady(it) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) refreshLayout()
    }

    override fun onInterrupt() = Unit

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshLayout()
    }

    /** El usuario o el sistema desactivan el servicio: los botones vuelven antes de irse. */
    override fun onUnbind(intent: Intent?): Boolean {
        shutdown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    private fun shutdown() {
        if (stopping) return
        stopping = true
        val active = overlay != null
        if (active) {
            // Primero dejar de escuchar: si no, nuestro propio 0 nos haría volver a ocultarlos.
            contentResolver.unregisterContentObserver(forceFsgObserver)
            unregisterReceiver(receiver)
        }
        if (::compat.isInitialized) {
            val status = compat.controller.onNavigationStopping(compat.files.readPrefs())
            writeStatus(status)
            Log.i(TAG, "stopping: $status")
        }
        overlay?.detach()
        overlay = null
    }

    /** Aplica una decisión de la capa 2 con las preferencias actuales y la deja anotada. */
    private fun decide(step: (GestureModePrefs) -> GestureModeStatus) {
        if (stopping || overlay == null) return
        val status = step(compat.files.readPrefs())
        writeStatus(status)
        Log.i(TAG, "mode: $status")
    }

    private fun writeStatus(status: GestureModeStatus) {
        compat.files.writeStatus(NavigationStatus(status, System.currentTimeMillis()))
    }

    private fun perform(action: NavAction) {
        val global =
            when (action) {
                NavAction.Back -> GLOBAL_ACTION_BACK
                NavAction.Home -> GLOBAL_ACTION_HOME
                NavAction.Recents -> GLOBAL_ACTION_RECENTS
            }
        val done = performGlobalAction(global)
        Log.i(TAG, "action: $action ok=$done")
    }

    private fun refreshLayout() {
        if (stopping) return
        overlay?.update(layoutStrips(geometry.layoutInput(windows)))
    }

    private companion object {
        const val TAG = "WhNavigation"
    }
}
