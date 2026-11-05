package com.wh01s17.launcher.core.input.binding

import com.wh01s17.launcher.core.input.CommandDirection
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.core.input.MediaKey
import com.wh01s17.launcher.core.input.WorkspaceCommand

/** Un problema al leer un comando: [arg] es el argumento culpable, o `null` si es el nombre. */
data class CommandProblem(
    val arg: String?,
    val message: String,
)

sealed interface CommandParse {
    data class Valid(
        val command: LauncherCommand,
    ) : CommandParse

    data class Invalid(
        val problems: List<CommandProblem>,
    ) : CommandParse
}

/**
 * Nombres de los comandos en `bindings` (`"command": "workspace.focus", "args": {"id": 1}`) y cómo se
 * convierten en [LauncherCommand]. Los argumentos llegan ya como `Int`, `Boolean` o `String`.
 */
object CommandCatalog {
    private val WORKSPACE = 1..10
    private val RESIZE = -1000..1000
    private val VOLUME_STEP = -10..10

    private val SPECS: List<CommandSpec> =
        listOf(
            CommandSpec("menu.open") { LauncherCommand.OpenMenu },
            CommandSpec("menu.apps") { LauncherCommand.OpenAppsMenu },
            CommandSpec("menu.system") { LauncherCommand.OpenSystemMenu },
            CommandSpec("notifications.open") { LauncherCommand.OpenNotificationShade },
            CommandSpec("notifications.panel") { LauncherCommand.ToggleNotificationPanel },
            CommandSpec("media.play-pause") { LauncherCommand.ControlMedia(MediaKey.PlayPause) },
            CommandSpec("media.next") { LauncherCommand.ControlMedia(MediaKey.Next) },
            CommandSpec("media.previous") { LauncherCommand.ControlMedia(MediaKey.Previous) },
            CommandSpec("shortcuts.show") { LauncherCommand.ShowShortcuts },
            CommandSpec("terminal.open") { LauncherCommand.OpenTerminal },
            CommandSpec("audio.volume", "step") { LauncherCommand.AdjustVolume(it.int("step", VOLUME_STEP)) },
            CommandSpec("workspace.focus", "id") { WorkspaceCommand.FocusWorkspace(it.int("id", WORKSPACE)) },
            CommandSpec("workspace.next") { WorkspaceCommand.CycleWorkspace(forward = true) },
            CommandSpec("workspace.previous") { WorkspaceCommand.CycleWorkspace(forward = false) },
            CommandSpec("workspace.layout") { WorkspaceCommand.ToggleLayout },
            CommandSpec("tile.move", "id", "follow") {
                WorkspaceCommand.MoveTileToWorkspace(it.int("id", WORKSPACE), it.bool("follow", default = true))
            },
            CommandSpec("tile.focus", "direction") { WorkspaceCommand.MoveFocus(it.direction("direction")) },
            CommandSpec("tile.swap", "direction") { WorkspaceCommand.SwapTile(it.direction("direction")) },
            CommandSpec("tile.resize", "dx", "dy") {
                WorkspaceCommand.ResizeTile(it.int("dx", RESIZE, default = 0), it.int("dy", RESIZE, default = 0))
            },
            CommandSpec("tile.float") { WorkspaceCommand.ToggleFloating },
            CommandSpec("tile.split") { WorkspaceCommand.ToggleSplit },
            CommandSpec("tile.fullscreen") { WorkspaceCommand.ToggleFullscreen },
            CommandSpec("tile.close") { WorkspaceCommand.CloseTile },
            CommandSpec("group.next") { WorkspaceCommand.CycleGroup(forward = true) },
            CommandSpec("group.previous") { WorkspaceCommand.CycleGroup(forward = false) },
            CommandSpec("group.toggle") { WorkspaceCommand.ToggleGroup },
            CommandSpec("group.move-into", "direction") { WorkspaceCommand.MoveIntoGroup(it.direction("direction")) },
            CommandSpec("group.move-out") { WorkspaceCommand.MoveOutOfGroup },
            CommandSpec("scratchpad.toggle") { WorkspaceCommand.ToggleScratchpad },
            CommandSpec("scratchpad.move") { WorkspaceCommand.MoveTileToScratchpad },
            CommandSpec("edit.toggle") { WorkspaceCommand.ToggleEditMode },
        )

    private val BY_NAME = SPECS.associateBy { it.name }

    /** Todos los nombres, en el orden en que se documentan. */
    val names: List<String> get() = SPECS.map { it.name }

    fun parse(
        name: String,
        args: Map<String, Any> = emptyMap(),
    ): CommandParse {
        val spec =
            BY_NAME[name] ?: return CommandParse.Invalid(listOf(CommandProblem(null, "«$name» no es un comando; ver docs/bindings.md")))
        val reader = ArgsReader(args)
        val unknown = args.keys.filter { it !in spec.args }.map { CommandProblem(it, "«${spec.name}» no usa este argumento") }
        val command = spec.build(reader)
        val problems = unknown + reader.problems
        return if (problems.isEmpty()) CommandParse.Valid(command) else CommandParse.Invalid(problems)
    }
}

private class CommandSpec(
    val name: String,
    vararg args: String,
    val build: (ArgsReader) -> LauncherCommand,
) {
    val args: Set<String> = args.toSet()
}

/** Lee argumentos anotando los problemas; devuelve un valor cualquiera válido si hay error (no se usa). */
private class ArgsReader(
    private val args: Map<String, Any>,
) {
    val problems = mutableListOf<CommandProblem>()

    fun int(
        key: String,
        range: IntRange,
        default: Int? = null,
    ): Int {
        val value = args[key]
        val message =
            when {
                value == null -> if (default == null) "falta" else return default
                value !is Int -> "debe ser un número entero"
                value !in range -> "debe estar entre ${range.first} y ${range.last}"
                else -> return value
            }
        problems += CommandProblem(key, message)
        return range.first
    }

    fun bool(
        key: String,
        default: Boolean,
    ): Boolean =
        when (val value = args[key]) {
            null -> default
            is Boolean -> value
            else -> default.also { problems += CommandProblem(key, "debe ser true o false") }
        }

    fun direction(key: String): CommandDirection {
        val value = args[key]
        val direction = (value as? String)?.let { name -> CommandDirection.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } }
        if (direction != null) return direction
        val options = CommandDirection.entries.joinToString { it.name.lowercase() }
        problems += CommandProblem(key, if (value == null) "falta; opciones: $options" else "«$value» no es válida; opciones: $options")
        return CommandDirection.Left
    }
}
