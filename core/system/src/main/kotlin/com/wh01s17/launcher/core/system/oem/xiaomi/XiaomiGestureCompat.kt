package com.wh01s17.launcher.core.system.oem.xiaomi

import android.Manifest
import android.app.role.RoleManager
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Capa 2 de plan.md §5.9 para un [Context]: el controlador con los ajustes reales, el rol HOME y
 * los archivos compartidos. Cada proceso crea la suya; no guarda estado propio salvo el límite de
 * reescrituras del controlador.
 */
class XiaomiGestureCompat(
    context: Context,
    clock: () -> Long = System::currentTimeMillis,
) {
    private val appContext = context.applicationContext

    val settings: SecureSettingsGateway = AndroidSecureSettings(appContext)
    val files = NavigationFiles(appContext.noBackupFilesDir)
    val controller = GestureModeController(settings, ::isDefaultHome, clock)

    /** `true` si este launcher tiene el rol HOME (es el predeterminado). */
    fun isDefaultHome(): Boolean = appContext.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) == true

    companion object {
        /** Acción del aviso "cambió el modo" del launcher al proceso de navegación. */
        fun modeChangedAction(context: Context) = "${context.packageName}.NAVIGATION_MODE_CHANGED"

        /** Aviso "cambiaron los ajustes de las franjas" ([com.wh01s17.launcher.core.system.navigation.EdgeSettings]). */
        fun edgesChangedAction(context: Context) = "${context.packageName}.NAVIGATION_EDGES_CHANGED"
    }
}

private const val TAG = "WhGestureCompat"

/** [SecureSettingsGateway] sobre `Settings.Global`/`Settings.Secure`. */
internal class AndroidSecureSettings(
    private val context: Context,
) : SecureSettingsGateway {
    private val resolver: ContentResolver = context.contentResolver

    override fun canWrite(): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    override fun read(setting: SystemSetting): Int? =
        try {
            when (setting.table) {
                SettingsTable.Global -> Settings.Global.getInt(resolver, setting.key)
                SettingsTable.Secure -> Settings.Secure.getInt(resolver, setting.key)
            }
        } catch (ignored: Settings.SettingNotFoundException) {
            null // HyperOS no lo define hasta que alguien lo escribe
        }

    override fun write(
        setting: SystemSetting,
        value: Int,
    ): Boolean {
        if (!canWrite()) return false
        return try {
            when (setting.table) {
                SettingsTable.Global -> Settings.Global.putInt(resolver, setting.key, value)
                SettingsTable.Secure -> Settings.Secure.putInt(resolver, setting.key, value)
            }
        } catch (e: SecurityException) {
            // HyperOS puede negar la escritura aunque el permiso figure como concedido.
            Log.w(TAG, "cannot write ${setting.key}", e)
            false
        }
    }
}

/** Datos del teléfono para el asistente (plan.md §5.9: fabricante y versión de MIUI/HyperOS). */
data class DeviceInfo(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val sdk: Int,
    /** `ro.mi.os.version.incremental` (HyperOS) o `ro.miui.ui.version.name` (MIUI); `null` si no hay. */
    val xiaomiOsVersion: String?,
) {
    val isXiaomi: Boolean
        get() = listOf(manufacturer, brand).any { it.lowercase() in XIAOMI_BRANDS }

    companion object {
        private val XIAOMI_BRANDS = setOf("xiaomi", "redmi", "poco")

        /** Lee `getprop`, que puede tardar: llamarlo fuera del hilo principal. */
        fun current(): DeviceInfo =
            DeviceInfo(
                manufacturer = Build.MANUFACTURER.orEmpty(),
                brand = Build.BRAND.orEmpty(),
                model = Build.MODEL.orEmpty(),
                sdk = Build.VERSION.SDK_INT,
                xiaomiOsVersion =
                    getprop("ro.mi.os.version.incremental")
                        ?: getprop("ro.miui.ui.version.name")?.let { "MIUI $it" },
            )

        private const val GETPROP_TIMEOUT_SECONDS = 2L

        private fun getprop(name: String): String? =
            try {
                val process = ProcessBuilder("getprop", name).redirectErrorStream(true).start()
                val output =
                    process.inputStream
                        .bufferedReader()
                        .use { it.readText() }
                        .trim()
                process.waitFor(GETPROP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                output.ifEmpty { null }
            } catch (ignored: IOException) {
                null // sin getprop: se usa sólo el fabricante
            } catch (ignored: InterruptedException) {
                Thread.currentThread().interrupt()
                null
            }
    }
}
