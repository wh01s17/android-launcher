package com.wh01s17.launcher.core.system.oem.xiaomi

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.wh01s17.launcher.core.system.navigation.NavigationWatchdog

/** Todo lo que muestra el asistente de compatibilidad (plan.md §5.9), en un instante. */
data class CompatibilitySnapshot(
    val packageName: String,
    val device: DeviceInfo,
    /** El servicio está activado en Ajustes → Accesibilidad. */
    val serviceEnabled: Boolean,
    /** Android lo tiene conectado ahora mismo (activado y vivo). */
    val serviceRunning: Boolean,
    val permissionGranted: Boolean,
    val isDefaultHome: Boolean,
    val forceFsgNavBar: Int?,
    val navigationMode: Int?,
    val prefs: GestureModePrefs,
    val status: NavigationStatus?,
    /** Última vez que la vigilancia tuvo que devolver los botones (en este arranque del launcher). */
    val lastRestoreAtMillis: Long?,
)

/** Lo que el asistente puede leer y cambiar; un doble lo sustituye en las pruebas. */
interface CompatibilityGateway {
    /** Bloqueante (lee ajustes y `getprop`): llamarlo fuera del hilo principal. */
    fun snapshot(): CompatibilitySnapshot

    fun setModeEnabled(enabled: Boolean)

    /** Avisa cuando cambian los ajustes de navegación o los servicios de accesibilidad. */
    fun observe(onChange: () -> Unit): AutoCloseable
}

/** [CompatibilityGateway] real, sobre la vigilancia del proceso del launcher. */
class AndroidCompatibilityGateway(
    context: Context,
    private val watchdog: NavigationWatchdog,
    private val compat: XiaomiGestureCompat = XiaomiGestureCompat(context),
) : CompatibilityGateway {
    private val appContext = context.applicationContext
    private val device by lazy { DeviceInfo.current() }

    override fun snapshot(): CompatibilitySnapshot =
        CompatibilitySnapshot(
            packageName = appContext.packageName,
            device = device,
            serviceEnabled = watchdog.isNavigationServiceEnabled(),
            serviceRunning = watchdog.isNavigationServiceRunning(),
            permissionGranted = compat.settings.canWrite(),
            isDefaultHome = compat.isDefaultHome(),
            forceFsgNavBar = compat.settings.read(SystemSetting.ForceFsgNavBar),
            navigationMode = compat.settings.read(SystemSetting.NavigationMode),
            prefs = compat.files.readPrefs(),
            status = compat.files.readStatus(),
            lastRestoreAtMillis = watchdog.lastRestoreAtMillis,
        )

    override fun setModeEnabled(enabled: Boolean) = watchdog.setModeEnabled(enabled)

    override fun observe(onChange: () -> Unit): AutoCloseable {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) = onChange()
            }
        val resolver = appContext.contentResolver
        val uris =
            listOf(
                Settings.Global.getUriFor(SystemSetting.ForceFsgNavBar.key),
                Settings.Secure.getUriFor(SystemSetting.NavigationMode.key),
                Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            )
        uris.forEach { resolver.registerContentObserver(it, false, observer) }
        return AutoCloseable { resolver.unregisterContentObserver(observer) }
    }
}

/**
 * Pantallas del sistema a las que manda el asistente. Cada una devuelve `false` si el teléfono no
 * la tiene (p. ej. *Inicio automático* fuera de MIUI/HyperOS).
 */
object SystemScreens {
    fun accessibility(context: Context) = start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    /** Ficha de la app: en HyperOS incluye *Ahorro de batería* e *Inicio automático*. */
    fun appDetails(context: Context) =
        start(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))

    /** Lista de *Inicio automático* de MIUI/HyperOS; si no existe, la ficha de la app. */
    fun autostart(context: Context) =
        start(context, Intent().setComponent(ComponentName(MIUI_SECURITY_CENTER, MIUI_AUTOSTART_ACTIVITY))) || appDetails(context)

    fun developerOptions(context: Context) = start(context, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))

    private fun start(
        context: Context,
        intent: Intent,
    ): Boolean =
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (ignored: ActivityNotFoundException) {
            false
        } catch (ignored: SecurityException) {
            false // actividad no exportada en esta versión de MIUI
        }

    private const val MIUI_SECURITY_CENTER = "com.miui.securitycenter"
    private const val MIUI_AUTOSTART_ACTIVITY = "com.miui.permcenter.autostart.AutoStartManagementActivity"
}
