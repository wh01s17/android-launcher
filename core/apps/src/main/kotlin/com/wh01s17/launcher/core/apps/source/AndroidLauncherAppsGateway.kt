package com.wh01s17.launcher.core.apps.source

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.ContextCompat
import com.wh01s17.launcher.core.apps.model.AppEntry
import com.wh01s17.launcher.core.apps.model.AppKey
import com.wh01s17.launcher.core.apps.model.AppProfile
import com.wh01s17.launcher.core.apps.model.ProfileKind
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

/**
 * [LauncherAppsGateway] real: `LauncherApps` para las apps y sus cambios, `UserManager` para los
 * perfiles y broadcasts del sistema para pausas de perfil y cambios de idioma.
 *
 * Un perfil puede desaparecer o bloquearse entre dos llamadas: las lecturas no lanzan, devuelven
 * lo que haya (o nada) y el siguiente aviso corrige el índice.
 */
class AndroidLauncherAppsGateway(
    context: Context,
) : LauncherAppsGateway {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val myUser = Process.myUserHandle()

    /** `UserHandle` de un perfil, para lanzar sus apps; `null` si ya no existe. */
    fun userHandle(serial: Long): UserHandle? = userManager.getUserForSerialNumber(serial)

    fun serialOf(user: UserHandle): Long = userManager.getSerialNumberForUser(user)

    override fun profiles(): List<AppProfile> =
        launcherApps.profiles
            .map { user -> AppProfile(serialOf(user), kindOf(user), isAvailable(user)) }
            .sortedBy { it.kind != ProfileKind.Personal }

    override fun activities(
        userSerial: Long,
        packageName: String?,
    ): List<AppEntry> {
        val user = userHandle(userSerial) ?: return emptyList()
        return runCatching { launcherApps.getActivityList(packageName, user) }
            .getOrDefault(emptyList())
            .map { it.toEntry(userSerial) }
    }

    override fun events(): Flow<AppEvent> =
        callbackFlow {
            val callback =
                object : LauncherApps.Callback() {
                    private fun changed(
                        user: UserHandle,
                        packages: Array<out String>,
                    ) {
                        trySend(AppEvent.PackagesChanged(serialOf(user), packages.toSet()))
                    }

                    override fun onPackageRemoved(
                        packageName: String,
                        user: UserHandle,
                    ) = changed(user, arrayOf(packageName))

                    override fun onPackageAdded(
                        packageName: String,
                        user: UserHandle,
                    ) = changed(user, arrayOf(packageName))

                    override fun onPackageChanged(
                        packageName: String,
                        user: UserHandle,
                    ) = changed(user, arrayOf(packageName))

                    override fun onPackagesAvailable(
                        packageNames: Array<out String>,
                        user: UserHandle,
                        replacing: Boolean,
                    ) = changed(user, packageNames)

                    override fun onPackagesUnavailable(
                        packageNames: Array<out String>,
                        user: UserHandle,
                        replacing: Boolean,
                    ) = changed(user, packageNames)

                    override fun onPackagesSuspended(
                        packageNames: Array<out String>,
                        user: UserHandle,
                    ) = changed(user, packageNames)

                    override fun onPackagesUnsuspended(
                        packageNames: Array<out String>,
                        user: UserHandle,
                    ) = changed(user, packageNames)
                }
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context,
                        intent: Intent,
                    ) {
                        val event = if (intent.action == Intent.ACTION_LOCALE_CHANGED) AppEvent.LocaleChanged else AppEvent.ProfilesChanged
                        trySend(event)
                    }
                }
            launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
            // Sólo llegan broadcasts del sistema: el receptor no necesita estar exportado.
            ContextCompat.registerReceiver(appContext, receiver, systemEventsFilter(), ContextCompat.RECEIVER_NOT_EXPORTED)
            awaitClose {
                launcherApps.unregisterCallback(callback)
                appContext.unregisterReceiver(receiver)
            }
        }.buffer(Channel.UNLIMITED) // Ningún aviso se descarta aunque el índice tarde en releerse.

    private fun kindOf(user: UserHandle): ProfileKind =
        when {
            user == myUser -> ProfileKind.Personal
            // Antes de Android 15 sólo había perfiles de trabajo (no existía el espacio privado).
            Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM -> ProfileKind.Work
            else ->
                when (runCatching { launcherApps.getLauncherUserInfo(user)?.userType }.getOrNull()) {
                    UserManager.USER_TYPE_PROFILE_MANAGED, null -> ProfileKind.Work
                    UserManager.USER_TYPE_PROFILE_PRIVATE -> ProfileKind.Private
                    else -> ProfileKind.Other
                }
        }

    private fun isAvailable(user: UserHandle): Boolean =
        user == myUser ||
            runCatching { !userManager.isQuietModeEnabled(user) && userManager.isUserUnlocked(user) }.getOrDefault(true)

    private fun LauncherActivityInfo.toEntry(userSerial: Long) =
        AppEntry(
            key = AppKey(componentName.packageName, componentName.className, userSerial),
            label = label?.toString().orEmpty().ifBlank { componentName.packageName },
            suspended = applicationInfo.flags and ApplicationInfo.FLAG_SUSPENDED != 0,
            iconStamp =
                applicationInfo.sourceDir
                    .orEmpty()
                    .hashCode()
                    .toLong(),
        )

    private fun systemEventsFilter() =
        IntentFilter().apply {
            addAction(Intent.ACTION_LOCALE_CHANGED)
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                addAction(Intent.ACTION_PROFILE_ADDED)
                addAction(Intent.ACTION_PROFILE_REMOVED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                addAction(Intent.ACTION_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
            }
        }
}
