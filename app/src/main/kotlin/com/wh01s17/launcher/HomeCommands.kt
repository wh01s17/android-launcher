package com.wh01s17.launcher

import android.media.AudioManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.wh01s17.launcher.core.apps.actions.LaunchResult
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.core.input.WorkspaceCommand
import com.wh01s17.launcher.di.AppContainer
import com.wh01s17.launcher.feature.menu.panel.MenuPage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/**
 * Lo que hace la pantalla de inicio con cada [LauncherCommand] del bus (plan.md §5.3): teclado,
 * gestos, barra y menú llegan aquí. Los de escritorio van a [onWorkspace].
 */
internal class HomeCommands(
    private val activity: ComponentActivity,
    private val container: AppContainer,
    private val menu: () -> MenuController,
    private val onWorkspace: (WorkspaceCommand) -> Unit,
) {
    fun handle(command: LauncherCommand) {
        when (command) {
            LauncherCommand.OpenMenu -> menu().toggle(MenuPage.Root)
            LauncherCommand.OpenAppsMenu -> menu().toggle(MenuPage.Apps)
            LauncherCommand.OpenSystemMenu -> menu().toggle(MenuPage.System)
            LauncherCommand.OpenNotificationShade -> openNotificationShade()
            LauncherCommand.ShowShortcuts -> activity.startActivity(SettingsActivity.shortcuts(activity))
            LauncherCommand.OpenTerminal -> openTerminal()
            is LauncherCommand.AdjustVolume -> adjustVolume(command.steps)
            is WorkspaceCommand -> onWorkspace(command)
        }
    }

    /**
     * `Super + Return` (plan.md §3): la app de `apps.terminal`; si no está, lo dice. Justo al arrancar
     * el índice de apps puede no estar cargado (≈ 3 s en debug): se espera a la primera carga.
     */
    private fun openTerminal() {
        val spec =
            container.configRepository.state.value.config
                ?.apps
                ?.terminal ?: return
        activity.lifecycleScope.launch {
            val catalog = withTimeoutOrNull(CATALOG_WAIT_MS) { container.appRepository.catalog.first { it.loaded } }
            val result = catalog?.find(spec)?.let { container.appActions.launch(it) }
            if (result !is LaunchResult.Launched) {
                Toast.makeText(activity, activity.getString(R.string.terminal_missing, spec), Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Volumen multimedia con el panel del sistema, como las teclas de volumen del teléfono. */
    private fun adjustVolume(steps: Int) {
        val audio = activity.getSystemService(AudioManager::class.java) ?: return
        val direction = if (steps > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        repeat(abs(steps)) { audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI) }
    }

    /**
     * Sin el servicio de accesibilidad no se puede abrir la cortina: no se hace nada y se explica la
     * primera vez (plan.md §7, Hito 3), para no repetirlo en cada deslizamiento.
     */
    fun openNotificationShade() {
        if (container.notificationShade.open()) return
        if (container.oneTimeHints.shouldShow(HINT_SHADE)) Toast.makeText(activity, R.string.shade_needs_service, Toast.LENGTH_LONG).show()
    }

    private companion object {
        const val HINT_SHADE = "notification-shade-needs-service"
        const val CATALOG_WAIT_MS = 10_000L
    }
}
