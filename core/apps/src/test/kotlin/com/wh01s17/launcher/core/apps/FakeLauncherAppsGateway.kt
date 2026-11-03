package com.wh01s17.launcher.core.apps

import com.wh01s17.launcher.core.apps.model.AppEntry
import com.wh01s17.launcher.core.apps.model.AppKey
import com.wh01s17.launcher.core.apps.model.AppProfile
import com.wh01s17.launcher.core.apps.source.AppEvent
import com.wh01s17.launcher.core.apps.source.LauncherAppsGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

/** Doble de `LauncherApps`: perfiles y apps en memoria, avisos a mano y contadores de lecturas. */
class FakeLauncherAppsGateway(
    var profiles: List<AppProfile>,
    apps: List<AppEntry> = emptyList(),
) : LauncherAppsGateway {
    val apps = apps.toMutableList()
    private val events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 64)
    var fullReads = 0
        private set
    var packageReads = 0
        private set
    var subscribers = 0
        private set

    override fun profiles(): List<AppProfile> = profiles

    override fun activities(
        userSerial: Long,
        packageName: String?,
    ): List<AppEntry> {
        if (packageName == null) fullReads++ else packageReads++
        return apps.filter { it.key.userSerial == userSerial && (packageName == null || it.packageName == packageName) }
    }

    override fun events(): Flow<AppEvent> = events.onStart { subscribers++ }.onCompletion { subscribers-- }

    fun emit(event: AppEvent) {
        check(events.tryEmit(event))
    }
}

fun app(
    label: String,
    packageName: String,
    userSerial: Long = 0,
    className: String = "$packageName.MainActivity",
    suspended: Boolean = false,
) = AppEntry(AppKey(packageName, className, userSerial), label, suspended)
