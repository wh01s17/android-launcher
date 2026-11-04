package com.wh01s17.launcher.core.apps.model

import java.text.Collator
import java.util.Locale

/** Una actividad lanzable tal como la devuelve `LauncherApps`, ya sin tipos de Android. */
data class AppEntry(
    val key: AppKey,
    val label: String,
    /** La app está suspendida (control parental, Bienestar digital): se muestra atenuada. */
    val suspended: Boolean = false,
    /**
     * Cambia cuando la app se reinstala o actualiza (se deriva de la ruta de su APK, que Android
     * renueva en cada instalación). Forma parte de la clave de la caché de iconos.
     */
    val iconStamp: Long = 0,
    /** `false` en apps del sistema sin actualizar: no se pueden desinstalar. */
    val uninstallable: Boolean = true,
) {
    val packageName: String get() = key.packageName
}

/** Tipo de perfil de usuario (plan.md §5.8). */
enum class ProfileKind {
    /** El usuario que ejecuta el launcher. */
    Personal,

    /** Perfil de trabajo gestionado. */
    Work,

    /** Espacio privado (Android 15+). */
    Private,

    /** Otro perfil visible para el launcher (p. ej. clonado). */
    Other,
}

/**
 * Un perfil con apps. [available] es `false` cuando el perfil está en pausa (modo silencio del
 * perfil de trabajo) o bloqueado: sus apps se listan pero no se pueden abrir.
 */
data class AppProfile(
    val serial: Long,
    val kind: ProfileKind,
    val available: Boolean = true,
)

/** El índice de apps en un instante: perfiles y apps ordenadas por nombre. */
data class AppCatalog(
    val profiles: List<AppProfile>,
    val apps: List<AppEntry>,
    /** `false` hasta la primera carga, para distinguir "cargando" de "no hay apps". */
    val loaded: Boolean,
) {
    val personalSerial: Long? get() = profiles.firstOrNull { it.kind == ProfileKind.Personal }?.serial

    fun profile(serial: Long): AppProfile? = profiles.firstOrNull { it.serial == serial }

    /**
     * La app de la config (`apps.terminal`): un paquete (`com.termux`, su primera actividad, mejor del
     * perfil personal) o una actividad (`com.termux/.app.TermuxActivity[@serial]`, plan.md §6.1).
     */
    fun find(spec: String): AppEntry? {
        if ('/' !in spec) {
            val matches = apps.filter { it.packageName == spec }
            return matches.firstOrNull { it.key.userSerial == personalSerial } ?: matches.firstOrNull()
        }
        val key = AppKey.parse(spec, personalSerial ?: return null) ?: return null
        return apps.firstOrNull { it.key == key }
    }

    /**
     * Sustituye las apps de [packages] en el perfil [userSerial] por [fresh]. Así se aplica un
     * aviso de `LauncherApps.Callback` sin volver a leer todos los paquetes.
     */
    fun replacePackages(
        userSerial: Long,
        packages: Set<String>,
        fresh: List<AppEntry>,
        locale: Locale = Locale.getDefault(),
    ): AppCatalog {
        val kept = apps.filterNot { it.key.userSerial == userSerial && it.packageName in packages }
        return copy(apps = sorted(kept + fresh, profiles, locale))
    }

    companion object {
        val Loading = AppCatalog(profiles = emptyList(), apps = emptyList(), loaded = false)

        fun of(
            profiles: List<AppProfile>,
            apps: List<AppEntry>,
            locale: Locale = Locale.getDefault(),
        ): AppCatalog = AppCatalog(profiles, sorted(apps, profiles, locale), loaded = true)

        /**
         * Orden estable: por nombre según el idioma (sin distinguir mayúsculas ni tildes), luego
         * el perfil personal antes que los demás, y por último la clave para desempatar.
         */
        private fun sorted(
            apps: List<AppEntry>,
            profiles: List<AppProfile>,
            locale: Locale,
        ): List<AppEntry> {
            val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
            val profileOrder = profiles.withIndex().associate { (index, profile) -> profile.serial to index }
            return apps.distinctBy { it.key }.sortedWith(
                compareBy<AppEntry, String>(collator) { it.label }
                    .thenBy { profileOrder[it.key.userSerial] ?: Int.MAX_VALUE }
                    .thenBy { it.key.packageName }
                    .thenBy { it.key.className },
            )
        }
    }
}
