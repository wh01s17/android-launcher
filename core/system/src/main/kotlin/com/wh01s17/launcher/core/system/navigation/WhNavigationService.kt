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
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
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
 * Privacidad: pide ver las ventanas para saber si el teclado está abierto y no taparlo, y qué paquete
 * está activo; de los clics sólo mira el id del botón «Eliminar todo» de los recientes de HyperOS
 * ([RecentsClearGuard]). No lee el contenido de ninguna app.
 */
class WhNavigationService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val geometry by lazy { NavigationGeometry(this) }
    private lateinit var compat: XiaomiGestureCompat
    private var overlay: EdgeStripOverlay? = null
    private var stopping = false
    private val recentsGuard by lazy { RecentsClearGuard(packageName) }

    /**
     * Pasa la app activa a [recentsGuard]. Tras «Eliminar todo» se repite hasta que HyperOS abre la
     * app mantenida: su ventana tarda en tener contenido y después no llega otro cambio de ventanas.
     */
    private val activeAppCheck: Runnable =
        object : Runnable {
            override fun run() {
                handler.removeCallbacks(this)
                val now = SystemClock.uptimeMillis()
                val active = windows.activeAppPackage(packageName)
                if (active != null && recentsGuard.onActiveApp(active, now)) {
                    Log.i(TAG, "recents cleared from home: back home")
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
                if (recentsGuard.isPending(now)) handler.postDelayed(this, CLEAR_CHECK_MILLIS)
            }
        }

    /** Ajustes de las franjas ya «efectivos»: con los botones ocultos, *inicio* siempre activo. */
    private var edges = EdgeSettings()

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
                when (intent.action) {
                    XiaomiGestureCompat.modeChangedAction(context) -> {
                        refreshLayout(reloadEdges = true)
                        decide { compat.controller.onNavigationReady(it) }
                    }
                    XiaomiGestureCompat.edgesChangedAction(context) -> refreshLayout(reloadEdges = true)
                    NotificationShade.lockAction(context) -> performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                    NotificationShade.powerAction(context) -> performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
                    NotificationShade.action(context) -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                    else -> refreshLayout()
                }
            }
        }

    override fun onServiceConnected() {
        super.onServiceConnected()
        compat = XiaomiGestureCompat(this)
        edges = compat.effectiveEdges()
        val strips = EdgeStripOverlay(this, getSystemService(WindowManager::class.java), { geometry.configFor(it, edges) }, ::perform)
        if (!strips.attach(layoutStrips(geometry.layoutInput(windows, edges)))) {
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
                addAction(XiaomiGestureCompat.edgesChangedAction(this@WhNavigationService))
                addAction(NotificationShade.action(this@WhNavigationService))
                addAction(NotificationShade.lockAction(this@WhNavigationService))
                addAction(NotificationShade.powerAction(this@WhNavigationService))
            }
        // Los avisos del modo y de la cortina vienen del propio launcher (mismo UID): no hace falta exportar.
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        contentResolver.registerContentObserver(Settings.Global.getUriFor(SystemSetting.ForceFsgNavBar.key), false, forceFsgObserver)
        decide { compat.controller.onNavigationReady(it) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                refreshLayout()
                activeAppCheck.run()
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED ->
                if (event.isClearAllRecents()) {
                    recentsGuard.onClearAll(SystemClock.uptimeMillis())
                    activeAppCheck.run()
                }
        }
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
        handler.removeCallbacks(activeAppCheck)
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

    /** Recoloca las franjas; con [reloadEdges], antes relee sus ajustes (grosor, gestos, sensibilidad). */
    private fun refreshLayout(reloadEdges: Boolean = false) {
        if (stopping) return
        if (reloadEdges) {
            edges = compat.effectiveEdges()
            overlay?.reconfigure()
            Log.i(TAG, "edges: $edges")
        }
        overlay?.update(layoutStrips(geometry.layoutInput(windows, edges)))
    }

    private companion object {
        const val TAG = "WhNavigation"
        const val CLEAR_CHECK_MILLIS = 250L
    }
}

/** Con el modo *gestos wh01s17* activado los botones pueden estar ocultos: *inicio* no se apaga. */
private fun XiaomiGestureCompat.effectiveEdges(): EdgeSettings = files.readEdges().effective(buttonsHidden = files.readPrefs().enabled)

/**
 * Sólo el paquete de la app activa, para [RecentsClearGuard]; no se lee su contenido. Si ninguna lo
 * está aún, el de la primera ventana de app que no sea [self]: el launcher puede seguir listado
 * detrás de los recientes y sólo cuenta si está activo.
 */
private fun List<AccessibilityWindowInfo>.activeAppPackage(self: String): String? {
    val apps = filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
    apps
        .firstOrNull { it.isActive }
        ?.root
        ?.packageName
        ?.let { return it.toString() }
    return apps.firstNotNullOfOrNull { window ->
        window.root
            ?.packageName
            ?.toString()
            ?.takeIf { it != self }
    }
}

/** El botón «Eliminar todo» de los recientes de HyperOS; de otras apps no se mira nada. */
private fun AccessibilityEvent.isClearAllRecents(): Boolean =
    packageName == RecentsClearGuard.MIUI_HOME && source?.viewIdResourceName == RecentsClearGuard.CLEAR_ALL_ID
