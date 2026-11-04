package com.wh01s17.launcher.core.config

import com.wh01s17.launcher.core.input.binding.Binding
import com.wh01s17.launcher.core.input.binding.BindingTrigger
import com.wh01s17.launcher.core.input.binding.ChordParse
import com.wh01s17.launcher.core.input.binding.CommandCatalog
import com.wh01s17.launcher.core.input.binding.CommandParse
import com.wh01s17.launcher.core.input.binding.Gesture
import com.wh01s17.launcher.core.input.binding.KeyChord
import com.wh01s17.launcher.core.input.binding.KeyModifier
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/**
 * `bindings` de `launcher.json` (plan.md §6.4). La lista del usuario se suma a la de fábrica: una
 * entrada con el mismo disparador la sustituye y `"command": "none"` la quita. Así basta con
 * escribir los cambios, como en `bindings.lua` (que añade a los atajos por defecto).
 */
internal object BindingSections {
    /** Comando especial: quita el atajo de fábrica con ese disparador. */
    const val UNBIND = "none"

    fun readBindings(
        element: JsonElement?,
        defaults: List<Binding>,
        errors: MutableList<ConfigError>,
    ): List<Binding> {
        val entries =
            when (element) {
                null -> return defaults
                is JsonArray -> element
                else -> return defaults.also { errors += ConfigError("bindings", "debe ser una lista") }
            }
        val seen = mutableSetOf<BindingTrigger>()
        val overrides = linkedMapOf<BindingTrigger, Binding?>()
        entries.forEachIndexed { index, entry ->
            val path = "bindings[$index]"
            val (trigger, binding) = readEntry(entry, path, errors) ?: return@forEachIndexed
            if (!seen.add(trigger)) {
                errors += ConfigError(path, "«$trigger» ya tiene un atajo más arriba en la lista")
            } else {
                overrides[trigger] = binding
            }
        }
        val kept = defaults.mapNotNull { default -> if (default.trigger in overrides) overrides[default.trigger] else default }
        val defaultTriggers = defaults.mapTo(mutableSetOf()) { it.trigger }
        return kept + overrides.filterKeys { it !in defaultTriggers }.values.filterNotNull()
    }

    /** El disparador y el atajo (`null` para [UNBIND]), o `null` si la entrada tiene errores. */
    private fun readEntry(
        element: JsonElement,
        path: String,
        errors: MutableList<ConfigError>,
    ): Pair<BindingTrigger, Binding?>? {
        val entry =
            element as? JsonObject
                ?: return null.also { errors += ConfigError(path, "debe ser un objeto con «keys» o «gesture» y «command»") }
        val trigger = readTrigger(entry, path, errors)
        val name = ConfigValues.readString(entry["command"], "$path.command", errors)
        if (entry["command"] == null) errors += ConfigError("$path.command", "falta")
        val args = readArgs(entry["args"], "$path.args", errors)
        return if (trigger != null && name != null && args != null) toBinding(trigger, name, args, path, errors) else null
    }

    private fun toBinding(
        trigger: BindingTrigger,
        name: String,
        args: Map<String, Any>,
        path: String,
        errors: MutableList<ConfigError>,
    ): Pair<BindingTrigger, Binding?>? {
        if (name == UNBIND) {
            if (args.isNotEmpty()) errors += ConfigError("$path.args", "«$UNBIND» no lleva argumentos")
            return (trigger to null).takeIf { args.isEmpty() }
        }
        return when (val parsed = CommandCatalog.parse(name, args)) {
            is CommandParse.Valid -> trigger to Binding(trigger, parsed.command)
            is CommandParse.Invalid -> {
                for (problem in parsed.problems) {
                    errors += ConfigError(problem.arg?.let { "$path.args.$it" } ?: "$path.command", problem.message)
                }
                null
            }
        }
    }

    private fun readTrigger(
        entry: JsonObject,
        path: String,
        errors: MutableList<ConfigError>,
    ): BindingTrigger? {
        val keys = ConfigValues.readString(entry["keys"], "$path.keys", errors)
        val gesture = ConfigValues.readString(entry["gesture"], "$path.gesture", errors)
        if (entry.containsKey("keys") == entry.containsKey("gesture")) {
            errors += ConfigError(path, "necesita «keys» o «gesture» (sólo uno)")
            return null
        }
        if (keys != null) {
            return when (val chord = KeyChord.parse(keys)) {
                is ChordParse.Valid -> BindingTrigger.Keys(chord.chord)
                is ChordParse.Invalid -> null.also { errors += ConfigError("$path.keys", chord.message) }
            }
        }
        gesture ?: return null
        val touch = Gesture.fromConfig(gesture)
        if (touch == null) {
            errors += ConfigError("$path.gesture", "«$gesture» no es un gesto; opciones: ${Gesture.entries.joinToString { it.configName }}")
        }
        return touch?.let(BindingTrigger::Touch)
    }

    /** `args` como valores de Kotlin (`Int`, `Boolean`, `String`); vacío si falta, `null` si hay errores. */
    private fun readArgs(
        element: JsonElement?,
        path: String,
        errors: MutableList<ConfigError>,
    ): Map<String, Any>? {
        val args =
            when (element) {
                null -> return emptyMap()
                is JsonObject -> element
                else -> return null.also { errors += ConfigError(path, "debe ser un objeto") }
            }
        val values = mutableMapOf<String, Any>()
        for ((key, value) in args) {
            val primitive = value as? JsonPrimitive
            val converted =
                when {
                    primitive == null -> null
                    primitive.isString -> primitive.content
                    else -> primitive.booleanOrNull ?: primitive.intOrNull
                }
            if (converted ==
                null
            ) {
                errors += ConfigError("$path.$key", "debe ser un texto, un número entero o true/false")
            } else {
                values[key] = converted
            }
        }
        return values.takeIf { it.size == args.size }
    }

    /** `keyboard.superAlternative`: `ALT`, `CTRL` o `none`. Lo que falta conserva [defaults]. */
    fun readKeyboard(
        element: JsonElement?,
        defaults: KeyboardConfig,
        errors: MutableList<ConfigError>,
    ): KeyboardConfig {
        if (element != null && element !is JsonObject) errors += ConfigError("keyboard", "debe ser un objeto")
        val path = "keyboard.superAlternative"
        val name = (element as? JsonObject)?.let { ConfigValues.readString(it["superAlternative"], path, errors) }
        return if (name == null) defaults else KeyboardConfig(readAlternative(name, path, errors))
    }

    private fun readAlternative(
        name: String,
        path: String,
        errors: MutableList<ConfigError>,
    ): KeyModifier? {
        if (name.equals(UNBIND, ignoreCase = true)) return null
        val modifier = SUPER_ALTERNATIVES.firstOrNull { it.configName.equals(name, ignoreCase = true) }
        if (modifier == null) {
            errors += ConfigError(path, "«$name» no es válido; opciones: ${SUPER_ALTERNATIVES.joinToString { it.configName }}, $UNBIND")
        }
        return modifier
    }

    /** `SHIFT` no sirve: con el menú abierto es escribir mayúsculas. */
    private val SUPER_ALTERNATIVES = listOf(KeyModifier.Alt, KeyModifier.Ctrl)
}
