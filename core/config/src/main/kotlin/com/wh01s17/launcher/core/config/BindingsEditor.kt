package com.wh01s17.launcher.core.config

import com.wh01s17.launcher.core.input.binding.Binding
import com.wh01s17.launcher.core.input.binding.BindingTrigger
import com.wh01s17.launcher.core.input.binding.encode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Un cambio del editor de atajos (Ajustes → Atajos, plan.md §7 Hito 6). */
sealed interface BindingEdit {
    /** El comando de [from] pasa a [to]; si [to] ya tenía atajo, lo pierde. */
    data class Rebind(
        val from: BindingTrigger,
        val to: BindingTrigger,
    ) : BindingEdit

    /** [trigger] deja de tener atajo. */
    data class Remove(
        val trigger: BindingTrigger,
    ) : BindingEdit

    /** Vuelven los atajos de fábrica: se borra `bindings` del archivo del usuario. */
    data object ResetAll : BindingEdit
}

/**
 * Aplica un [BindingEdit] y escribe en `launcher.json` sólo la diferencia con la fábrica, como la
 * escribiría el usuario (plan.md §6.4): las entradas que cambian o se añaden y `"none"` para las de
 * fábrica que se quitan. El resto del archivo se conserva (reescrito con sangría).
 */
internal object BindingsEditor {
    private val json = Json { prettyPrint = true }

    fun apply(
        current: List<Binding>,
        edit: BindingEdit,
    ): List<Binding> =
        when (edit) {
            is BindingEdit.Remove -> current.filter { it.trigger != edit.trigger }
            is BindingEdit.Rebind -> {
                val command = current.firstOrNull { it.trigger == edit.from }?.command
                if (command == null || edit.from == edit.to) {
                    current
                } else {
                    current.mapNotNull { binding ->
                        when (binding.trigger) {
                            edit.from -> Binding(edit.to, command)
                            edit.to -> null
                            else -> binding
                        }
                    }
                }
            }
            BindingEdit.ResetAll -> current
        }

    /** El `launcher.json` del usuario con [bindings] en lugar de lo que tuviera (o sin `bindings` si son los de fábrica). */
    fun write(
        userText: String?,
        factory: List<Binding>,
        bindings: List<Binding>,
    ): String {
        val root = userText?.let { json.parseToJsonElement(it) as? JsonObject } ?: JsonObject(mapOf("version" to JsonPrimitive(1)))
        val delta = delta(factory, bindings)
        val updated = if (delta.isEmpty()) root - "bindings" else root + ("bindings" to JsonArray(delta))
        return json.encodeToString(JsonElement.serializer(), JsonObject(updated)) + "\n"
    }

    private fun delta(
        factory: List<Binding>,
        bindings: List<Binding>,
    ): List<JsonElement> {
        val wanted = bindings.associateBy { it.trigger }
        val factoryByTrigger = factory.associateBy { it.trigger }
        val changed = bindings.filter { factoryByTrigger[it.trigger] != it }.map(::entry)
        val removed = factory.filter { it.trigger !in wanted }.map { unbind(it.trigger) }
        return changed + removed
    }

    private fun entry(binding: Binding): JsonObject {
        val command = binding.command.encode()
        return buildJsonObject {
            putTrigger(binding.trigger)
            put("command", command.name)
            if (command.args.isNotEmpty()) put("args", JsonObject(command.args.mapValues { (_, value) -> primitive(value) }))
        }
    }

    private fun unbind(trigger: BindingTrigger) =
        buildJsonObject {
            putTrigger(trigger)
            put("command", BindingSections.UNBIND)
        }

    private fun JsonObjectBuilder.putTrigger(trigger: BindingTrigger) {
        when (trigger) {
            is BindingTrigger.Keys -> put("keys", trigger.chord.toString())
            is BindingTrigger.Touch -> put("gesture", trigger.gesture.configName)
        }
    }

    private fun primitive(value: Any): JsonPrimitive =
        when (value) {
            is Int -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            else -> JsonPrimitive(value.toString())
        }
}
