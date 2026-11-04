package com.wh01s17.launcher.feature.settings.shortcuts

import com.wh01s17.launcher.core.config.LauncherConfig
import com.wh01s17.launcher.core.input.binding.Availability
import com.wh01s17.launcher.core.input.binding.BindingTrigger
import com.wh01s17.launcher.core.input.binding.BlockReason
import com.wh01s17.launcher.core.input.binding.EncodedCommand
import com.wh01s17.launcher.core.input.binding.KeyBindingResolver
import com.wh01s17.launcher.core.input.binding.KeyModifier
import com.wh01s17.launcher.core.input.binding.encode

/** Una fila de la tabla: el disparador tal como está en la config y su comando. */
data class ShortcutRow(
    val trigger: BindingTrigger,
    /** Lo que hay que pulsar o hacer en este equipo (`ALT + 1` para `SUPER + 1`). */
    val press: String,
    /** Por qué no se puede pulsar, o `null` si se puede. */
    val blocked: BlockReason?,
    val command: EncodedCommand,
)

/**
 * La tabla de `Super + K` (plan.md §6.4), generada desde los `bindings` vigentes como
 * `keybindings-md.sh` en el escritorio.
 */
data class ShortcutTable(
    val keys: List<ShortcutRow>,
    val gestures: List<ShortcutRow>,
    val superAlternative: KeyModifier?,
    val superDelivered: Boolean,
) {
    companion object {
        fun of(
            config: LauncherConfig,
            superDelivered: Boolean,
        ): ShortcutTable {
            val resolver = KeyBindingResolver(config.bindings, config.keyboard.superAlternative)
            val rows =
                config.bindings.map { binding ->
                    when (val trigger = binding.trigger) {
                        is BindingTrigger.Keys ->
                            when (val availability = resolver.availability(trigger.chord, superDelivered)) {
                                is Availability.Press -> ShortcutRow(trigger, availability.chord.toString(), null, binding.command.encode())
                                is Availability.Blocked ->
                                    ShortcutRow(trigger, trigger.chord.toString(), availability.reason, binding.command.encode())
                            }
                        is BindingTrigger.Touch -> ShortcutRow(trigger, trigger.gesture.configName, null, binding.command.encode())
                    }
                }
            return ShortcutTable(
                keys = rows.filter { it.trigger is BindingTrigger.Keys },
                gestures = rows.filter { it.trigger is BindingTrigger.Touch },
                superAlternative = config.keyboard.superAlternative,
                superDelivered = superDelivered,
            )
        }
    }
}
