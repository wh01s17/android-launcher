package com.wh01s17.launcher.core.apps

import com.wh01s17.launcher.core.apps.model.AppCatalog
import com.wh01s17.launcher.core.apps.model.AppEntry
import com.wh01s17.launcher.core.apps.source.AppEvent
import com.wh01s17.launcher.core.apps.source.LauncherAppsGateway
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

/** Índice de apps de todos los perfiles (plan.md §5.3). */
interface AppRepository {
    /** Siempre ordenado por nombre; [AppCatalog.Loading] hasta la primera lectura. */
    val catalog: StateFlow<AppCatalog>
}

/**
 * [AppRepository] sobre `LauncherApps`: lee todo al empezar y, después, sólo los paquetes que
 * cambian. Un cambio de perfil o de idioma vuelve a leerlo todo.
 *
 * Los avisos se procesan en orden en [ioDispatcher]; mientras haya alguien recogiendo
 * [catalog] (según [started]) el callback de `LauncherApps` sigue registrado. Si se deja de
 * recoger y se vuelve, se relee todo, así que ningún aviso perdido deja el índice desfasado.
 */
class LauncherAppRepository(
    private val gateway: LauncherAppsGateway,
    scope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
    started: SharingStarted = SharingStarted.Eagerly,
    private val locale: () -> Locale = { Locale.getDefault() },
) : AppRepository {
    override val catalog: StateFlow<AppCatalog> =
        gateway
            .events()
            .onStart { emit(AppEvent.ProfilesChanged) }
            .runningFold(AppCatalog.Loading, ::apply)
            .flowOn(ioDispatcher)
            .stateIn(scope, started, AppCatalog.Loading)

    private fun apply(
        catalog: AppCatalog,
        event: AppEvent,
    ): AppCatalog =
        when (event) {
            AppEvent.ProfilesChanged, AppEvent.LocaleChanged -> loadAll()
            is AppEvent.PackagesChanged ->
                if (!catalog.loaded || catalog.profile(event.userSerial) == null) {
                    // Aviso de un perfil que aún no conocemos: lo más seguro es releerlo todo.
                    loadAll()
                } else {
                    val fresh = event.packages.flatMap { gateway.activities(event.userSerial, it) }
                    catalog.replacePackages(event.userSerial, event.packages, fresh, locale())
                }
        }

    private fun loadAll(): AppCatalog {
        val profiles = gateway.profiles()
        val apps: List<AppEntry> = profiles.flatMap { gateway.activities(it.serial) }
        return AppCatalog.of(profiles, apps, locale())
    }
}
