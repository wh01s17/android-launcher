package com.wh01s17.launcher.core.apps.actions

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import com.wh01s17.launcher.core.apps.model.AppEntry
import com.wh01s17.launcher.core.apps.model.AppKey
import com.wh01s17.launcher.core.apps.source.AndroidLauncherAppsGateway

/**
 * [AppActions] sobre `LauncherApps`. Nada de esto mata procesos ni actúa sin el usuario: abrir,
 * mostrar la info del sistema o su diálogo de desinstalación (plan.md §2.2).
 *
 * Abrir una app de un perfil en pausa pide al sistema reanudarlo (Android deja hacerlo al launcher
 * por defecto) en vez de fallar en silencio.
 */
class LauncherAppActions(
    context: Context,
    private val gateway: AndroidLauncherAppsGateway,
) : AppActions {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)

    override fun launch(
        app: AppEntry,
        origin: LaunchOrigin?,
    ): LaunchResult =
        withUser(app.key) { user ->
            launcherApps.startMainActivity(app.key.component(), user, origin?.screenBounds(), origin?.options())
        }

    override fun openAppInfo(
        app: AppEntry,
        origin: LaunchOrigin?,
    ): LaunchResult =
        withUser(app.key) { user ->
            launcherApps.startAppDetailsActivity(app.key.component(), user, origin?.screenBounds(), origin?.options())
        }

    override fun requestUninstall(app: AppEntry): LaunchResult {
        if (!app.uninstallable) return LaunchResult.Failed("system app")
        return withUser(app.key) { user ->
            val intent =
                Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
                    .putExtra(Intent.EXTRA_USER, user)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            appContext.startActivity(intent)
        }
    }

    override fun canReadShortcuts(): Boolean = runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)

    override fun shortcuts(
        app: AppEntry,
        limit: Int,
    ): List<AppShortcut> {
        val user = gateway.userHandle(app.key.userSerial)
        if (user == null || !canReadShortcuts()) return emptyList()
        val query =
            LauncherApps
                .ShortcutQuery()
                .setPackage(app.packageName)
                .setActivity(app.key.component())
                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST)
        // Falla si el perfil está bloqueado o dejamos de ser el HOME entre la comprobación y la lectura.
        val infos = runCatching { launcherApps.getShortcuts(query, user) }.getOrNull().orEmpty()
        return infos.map { it.toShortcut(app.key) }.forMenu(limit)
    }

    override fun launchShortcut(
        shortcut: AppShortcut,
        origin: LaunchOrigin?,
    ): LaunchResult =
        withUser(shortcut.app) { user ->
            launcherApps.startShortcut(shortcut.app.packageName, shortcut.id, origin?.screenBounds(), origin?.options(), user)
        }

    /**
     * Ejecuta [action] con el `UserHandle` de [key]. Comprueba antes que el perfil exista y no
     * esté en pausa, y convierte los errores del sistema en [LaunchResult.Failed].
     */
    private inline fun withUser(
        key: AppKey,
        action: (UserHandle) -> Unit,
    ): LaunchResult {
        val user = gateway.userHandle(key.userSerial)
        return when {
            user == null -> LaunchResult.Failed("profile removed")
            isPaused(user) -> LaunchResult.ProfilePaused(resumeRequested = requestResume(user))
            else ->
                try {
                    action(user)
                    LaunchResult.Launched
                } catch (e: ActivityNotFoundException) {
                    LaunchResult.Failed(e.message ?: "activity not found")
                } catch (e: SecurityException) {
                    LaunchResult.Failed(e.message ?: "not allowed")
                } catch (e: IllegalStateException) {
                    // `startShortcut` sin ser el HOME por defecto, o un atajo que desapareció.
                    LaunchResult.Failed(e.message ?: "illegal state")
                }
        }
    }

    private fun isPaused(user: UserHandle): Boolean = runCatching { userManager.isQuietModeEnabled(user) }.getOrDefault(false)

    private fun requestResume(user: UserHandle): Boolean = runCatching { userManager.requestQuietModeEnabled(false, user) }.isSuccess

    private fun AppKey.component() = ComponentName(packageName, className)

    private fun ShortcutInfo.toShortcut(app: AppKey) =
        AppShortcut(
            id = id,
            app = app,
            shortLabel = shortLabel?.toString().orEmpty().ifBlank { id },
            longLabel = longLabel?.toString()?.takeIf { it.isNotBlank() },
            enabled = isEnabled,
            disabledMessage = disabledMessage?.toString(),
            rank = rank,
            fromManifest = isDeclaredInManifest,
        )
}

/** El icono en coordenadas de pantalla, que es lo que espera `sourceBounds`. */
private fun LaunchOrigin.screenBounds(): Rect {
    val location = IntArray(2).also { view.getLocationOnScreen(it) }
    return Rect(bounds).apply { offset(location[0], location[1]) }
}

/** La ventana nueva crece desde el icono. */
private fun LaunchOrigin.options(): Bundle =
    ActivityOptions.makeScaleUpAnimation(view, bounds.left, bounds.top, bounds.width(), bounds.height()).toBundle()
