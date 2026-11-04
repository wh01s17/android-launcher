package com.wh01s17.launcher.core.system.oem.xiaomi

import com.wh01s17.launcher.core.system.navigation.EdgeSensitivity
import com.wh01s17.launcher.core.system.navigation.EdgeSettings
import java.io.File
import java.io.IOException
import java.util.Properties

/**
 * Estado compartido entre el proceso del launcher y el de navegación (`:nav`). `SharedPreferences`
 * no se sincroniza entre procesos, así que cada dato es un archivo pequeño con **un solo
 * escritor**, escrito de forma atómica (temporal + `rename`) y leído de nuevo en cada decisión.
 */
class NavigationFiles(
    /** Carpeta de los archivos; quien quiera enterarse de cambios la vigila (`FileObserver`). */
    val directory: File,
) {
    private val prefsFile = File(directory, "navigation-mode.properties")
    private val statusFile = File(directory, STATUS_FILE_NAME)
    private val edgesFile = File(directory, "navigation-edges.properties")

    /** Lo escribe el launcher (asistente de compatibilidad). */
    fun readPrefs(): GestureModePrefs {
        val p = load(prefsFile) ?: return GestureModePrefs()
        return GestureModePrefs(
            enabled = p.getProperty(ENABLED).toBoolean(),
            hideGestureLine = p.getProperty(HIDE_LINE)?.toBoolean() ?: true,
            previousGestureLine = p.getProperty(PREVIOUS_LINE)?.toIntOrNull(),
        )
    }

    fun writePrefs(prefs: GestureModePrefs) {
        val p =
            Properties().apply {
                setProperty(ENABLED, prefs.enabled.toString())
                setProperty(HIDE_LINE, prefs.hideGestureLine.toString())
                prefs.previousGestureLine?.let { setProperty(PREVIOUS_LINE, it.toString()) }
            }
        store(prefsFile, p)
    }

    /** Ajustes de la capa 3; los escribe el launcher y los lee `:nav`. Un valor raro vuelve al de fábrica. */
    fun readEdges(): EdgeSettings {
        val p = load(edgesFile) ?: return EdgeSettings()
        val defaults = EdgeSettings()

        fun flag(
            key: String,
            default: Boolean,
        ) = p.getProperty(key)?.toBooleanStrictOrNull() ?: default
        return EdgeSettings(
            backLeft = flag(BACK_LEFT, defaults.backLeft),
            backRight = flag(BACK_RIGHT, defaults.backRight),
            home = flag(HOME, defaults.home),
            recents = flag(RECENTS, defaults.recents),
            sideThicknessDp = p.getProperty(SIDE)?.toIntOrNull() ?: defaults.sideThicknessDp,
            bottomThicknessDp = p.getProperty(BOTTOM)?.toIntOrNull() ?: defaults.bottomThicknessDp,
            sensitivity = EdgeSensitivity.entries.firstOrNull { it.name == p.getProperty(SENSITIVITY) } ?: defaults.sensitivity,
        )
    }

    fun writeEdges(settings: EdgeSettings) {
        val p =
            Properties().apply {
                setProperty(BACK_LEFT, settings.backLeft.toString())
                setProperty(BACK_RIGHT, settings.backRight.toString())
                setProperty(HOME, settings.home.toString())
                setProperty(RECENTS, settings.recents.toString())
                setProperty(SIDE, settings.sideThicknessDp.toString())
                setProperty(BOTTOM, settings.bottomThicknessDp.toString())
                setProperty(SENSITIVITY, settings.sensitivity.name)
            }
        store(edgesFile, p)
    }

    /** Lo escribe el proceso de navegación; el asistente lo muestra. */
    fun readStatus(): NavigationStatus? =
        load(statusFile)?.let { p ->
            GestureModeStatus.entries
                .firstOrNull { it.name == p.getProperty(STATUS) }
                ?.let { NavigationStatus(it, p.getProperty(AT)?.toLongOrNull() ?: 0) }
        }

    fun writeStatus(status: NavigationStatus) {
        store(
            statusFile,
            Properties().apply {
                setProperty(STATUS, status.status.name)
                setProperty(AT, status.atMillis.toString())
            },
        )
    }

    private fun load(file: File): Properties? =
        try {
            file.takeIf { it.isFile }?.inputStream()?.use { input -> Properties().apply { load(input) } }
        } catch (ignored: IOException) {
            null
        } catch (ignored: IllegalArgumentException) {
            null // archivo corrupto: como si no existiera
        }

    private fun store(
        file: File,
        properties: Properties,
    ) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.outputStream().use { properties.store(it, null) }
        if (!temp.renameTo(file)) throw IOException("cannot replace $file")
    }

    companion object {
        /** Lo escribe el proceso de navegación con temporal + `rename`: llega como `MOVED_TO`. */
        const val STATUS_FILE_NAME = "navigation-status.properties"
        private const val ENABLED = "enabled"
        private const val HIDE_LINE = "hideGestureLine"
        private const val PREVIOUS_LINE = "previousGestureLine"
        private const val STATUS = "status"
        private const val BACK_LEFT = "backLeft"
        private const val BACK_RIGHT = "backRight"
        private const val HOME = "home"
        private const val RECENTS = "recents"
        private const val SIDE = "sideThicknessDp"
        private const val BOTTOM = "bottomThicknessDp"
        private const val SENSITIVITY = "sensitivity"
        private const val AT = "at"
    }
}

/** Última decisión de la capa 2 y cuándo se tomó. */
data class NavigationStatus(
    val status: GestureModeStatus,
    val atMillis: Long,
)
