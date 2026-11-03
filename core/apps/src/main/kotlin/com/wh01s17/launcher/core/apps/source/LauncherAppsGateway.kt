package com.wh01s17.launcher.core.apps.source

import com.wh01s17.launcher.core.apps.model.AppEntry
import com.wh01s17.launcher.core.apps.model.AppProfile
import kotlinx.coroutines.flow.Flow

/**
 * Lo que el repositorio necesita de `LauncherApps` y `UserManager`, sin tipos de Android, para
 * poder probarlo con un doble. Las lecturas son bloqueantes: el repositorio las llama fuera del
 * hilo principal.
 */
interface LauncherAppsGateway {
    /** Perfiles visibles para el launcher; el personal, primero. */
    fun profiles(): List<AppProfile>

    /** Actividades lanzables del perfil; sólo las de [packageName] si se indica. */
    fun activities(
        userSerial: Long,
        packageName: String? = null,
    ): List<AppEntry>

    /** Avisos de cambios. Se registran al empezar a recoger y se quitan al cancelar. */
    fun events(): Flow<AppEvent>
}

/** Cambio que obliga a releer parte del índice. */
sealed interface AppEvent {
    /** Paquetes instalados, quitados, actualizados, suspendidos o (des)montados en un perfil. */
    data class PackagesChanged(
        val userSerial: Long,
        val packages: Set<String>,
    ) : AppEvent

    /** Un perfil apareció, desapareció, se pausó o se reanudó: se relee todo. */
    data object ProfilesChanged : AppEvent

    /** Cambió el idioma: los nombres de las apps cambian. */
    data object LocaleChanged : AppEvent
}
