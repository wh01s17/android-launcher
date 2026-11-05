package com.wh01s17.launcher.feature.settings.shortcuts

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.input.binding.BlockReason
import com.wh01s17.launcher.core.input.binding.EncodedCommand
import com.wh01s17.launcher.core.input.binding.Gesture
import com.wh01s17.launcher.feature.settings.R

/** El texto de un comando en la tabla, en el idioma de la app. */
@Composable
internal fun commandLabel(command: EncodedCommand): String {
    val (res, args) = labelOf(command)
    return stringResource(res, *args)
}

@Suppress("CyclomaticComplexMethod") // Una rama por comando del catálogo.
private fun labelOf(command: EncodedCommand): Pair<Int, Array<Any>> {
    val args = command.args
    val id = args["id"] as? Int ?: 0
    return when (command.name) {
        "menu.open" -> R.string.cmd_menu_open to emptyArray()
        "menu.apps" -> R.string.cmd_menu_apps to emptyArray()
        "menu.system" -> R.string.cmd_menu_system to emptyArray()
        "notifications.open" -> R.string.cmd_notifications to emptyArray()
        "notifications.panel" -> R.string.cmd_notification_panel to emptyArray()
        "media.play-pause" -> R.string.cmd_media_play_pause to emptyArray()
        "media.next" -> R.string.cmd_media_next to emptyArray()
        "media.previous" -> R.string.cmd_media_previous to emptyArray()
        "shortcuts.show" -> R.string.cmd_shortcuts to emptyArray()
        "wallpaper.open" -> R.string.style_wallpapers to emptyArray()
        "terminal.open" -> R.string.cmd_terminal to emptyArray()
        "audio.volume" -> (if ((args["step"] as? Int ?: 0) > 0) R.string.cmd_volume_up else R.string.cmd_volume_down) to emptyArray()
        "workspace.focus" -> R.string.cmd_workspace_focus to arrayOf(id)
        "workspace.next" -> R.string.cmd_workspace_next to emptyArray()
        "workspace.previous" -> R.string.cmd_workspace_previous to emptyArray()
        "workspace.layout" -> R.string.cmd_workspace_layout to emptyArray()
        "tile.move" -> (if (args["follow"] == false) R.string.cmd_tile_move_silent else R.string.cmd_tile_move) to arrayOf(id)
        "tile.focus" -> R.string.cmd_tile_focus to arrayOf(direction(args["direction"]))
        "tile.swap" -> R.string.cmd_tile_swap to arrayOf(direction(args["direction"]))
        "tile.resize" -> R.string.cmd_tile_resize to arrayOf(args["dx"] as? Int ?: 0, args["dy"] as? Int ?: 0)
        "tile.float" -> R.string.cmd_tile_float to emptyArray()
        "tile.split" -> R.string.cmd_tile_split to emptyArray()
        "tile.fullscreen" -> R.string.cmd_tile_fullscreen to emptyArray()
        "tile.close" -> R.string.cmd_tile_close to emptyArray()
        "group.next" -> R.string.cmd_group_next to emptyArray()
        "group.previous" -> R.string.cmd_group_previous to emptyArray()
        "edit.toggle" -> R.string.cmd_edit to emptyArray()
        "group.toggle" -> R.string.cmd_group_toggle to emptyArray()
        "group.move-into" -> R.string.cmd_group_move_into to arrayOf(direction(args["direction"]))
        "group.move-out" -> R.string.cmd_group_move_out to emptyArray()
        "scratchpad.toggle" -> R.string.cmd_scratchpad_toggle to emptyArray()
        "scratchpad.move" -> R.string.cmd_scratchpad_move to emptyArray()
        else -> R.string.cmd_unknown to arrayOf(command.name)
    }
}

/** Las flechas de la tabla, como en `KEYBINDINGS.md`. */
private fun direction(value: Any?): String =
    when (value) {
        "left" -> "←"
        "right" -> "→"
        "up" -> "↑"
        "down" -> "↓"
        else -> value.toString()
    }

@StringRes
internal fun gestureLabel(gesture: Gesture): Int =
    when (gesture) {
        Gesture.SwipeUp -> R.string.gesture_swipe_up
        Gesture.SwipeDown -> R.string.gesture_swipe_down
        Gesture.TwoFingerSwipeLeft -> R.string.gesture_two_left
        Gesture.TwoFingerSwipeRight -> R.string.gesture_two_right
        Gesture.TwoFingerSwipeUp -> R.string.gesture_two_up
        Gesture.TwoFingerSwipeDown -> R.string.gesture_two_down
        Gesture.DoubleTap -> R.string.gesture_double_tap
        Gesture.DoubleTapTile -> R.string.gesture_double_tap_tile
    }

@StringRes
internal fun blockLabel(reason: BlockReason): Int =
    when (reason) {
        BlockReason.NoSuper -> R.string.shortcuts_blocked_no_super
        BlockReason.AlternativeTwice -> R.string.shortcuts_blocked_twice
        BlockReason.Shadowed -> R.string.shortcuts_blocked_shadowed
        BlockReason.Recents -> R.string.shortcuts_blocked_recents
    }
