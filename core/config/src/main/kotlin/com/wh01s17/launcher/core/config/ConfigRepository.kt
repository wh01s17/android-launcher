package com.wh01s17.launcher.core.config

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import java.io.IOException

/** De dónde salen los textos de config; un doble los sustituye en las pruebas. */
interface ConfigSource {
    /** El `launcher.json` del usuario, o `null` si no existe. Bloqueante. */
    fun readUserConfig(): String?

    /** La config de fábrica, empaquetada en la app. Bloqueante. */
    fun readDefaultConfig(): String

    /** Sustituye el `launcher.json` del usuario de forma atómica (editor de atajos). Bloqueante. */
    fun writeUserConfig(text: String)
}

/** Resultado de [ConfigRepository.editBindings]. */
sealed interface EditResult {
    data object Saved : EditResult

    /** No se escribió nada: el archivo del usuario tiene errores, no se pudo leer o escribir, o el resultado no sería válido. */
    data class Refused(
        val errors: List<ConfigError>,
    ) : EditResult
}

/**
 * La config vigente. [config] es `null` sólo antes de la primera lectura; [errors] no está vacío
 * cuando el `launcher.json` del usuario no se pudo aplicar: entonces [config] es la última buena
 * (plan.md §6.2: una config inválida nunca deja el launcher inutilizable).
 */
data class ConfigState(
    val config: LauncherConfig? = null,
    val errors: List<ConfigError> = emptyList(),
)

class ConfigRepository(
    private val source: ConfigSource,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val mutableState = MutableStateFlow(ConfigState())
    val state: StateFlow<ConfigState> = mutableState.asStateFlow()

    /**
     * Lee de nuevo la config. Orden: si hay `launcher.json` y es válido, se usa; si no, se conserva
     * la última config buena (o la de fábrica la primera vez) y se anotan los errores.
     */
    suspend fun reload() =
        // Las recargas van en fila: si no, una que leyó el archivo antes podría terminar después y
        // pisar a una más reciente (arranque y `onResume` llegan casi a la vez).
        reloadLock.withLock {
            val next = withContext(ioDispatcher) { load(mutableState.value.config) }
            mutableState.value = next
        }

    private val reloadLock = Mutex()

    /**
     * Aplica un cambio del editor de atajos sobre el archivo del usuario y recarga. Nunca escribe
     * sobre un `launcher.json` con errores (se perderían cambios a medio hacer) ni un resultado que
     * no se pueda leer.
     */
    suspend fun editBindings(edit: BindingEdit): EditResult =
        reloadLock.withLock {
            val result = withContext(ioDispatcher) { writeEdit(edit) }
            if (result == EditResult.Saved) mutableState.value = withContext(ioDispatcher) { load(mutableState.value.config) }
            result
        }

    /** A style edit validates the existing document before merging only the owned keys. */
    suspend fun editValues(patch: JsonObject): EditResult =
        documentEdit { current ->
            when (val parsed = current?.let { ConfigParser.parse(it, defaults) }) {
                is ParseResult.Failure -> EditResult.Refused(parsed.errors) to null
                else -> validateDocument(ConfigDocuments.merge(current, patch))
            }
        }

    /** Import replaces only config, never workspace/widget state. Validation happens before writing. */
    suspend fun importDocument(text: String): EditResult = documentEdit { validateDocument(text) }

    /** Export the original document (including unknown keys), or the complete factory document. */
    suspend fun exportDocument(): String =
        reloadLock.withLock {
            withContext(ioDispatcher) { source.readUserConfig() ?: source.readDefaultConfig() }
        }

    private suspend fun documentEdit(transform: (String?) -> Pair<EditResult, String?>): EditResult =
        reloadLock.withLock {
            val result =
                withContext(ioDispatcher) {
                    try {
                        val (result, text) = transform(source.readUserConfig())
                        if (result == EditResult.Saved && text != null) source.writeUserConfig(text)
                        result
                    } catch (error: IOException) {
                        EditResult.Refused(listOf(ConfigError("launcher.json", "no se pudo guardar: ${error.message}")))
                    }
                }
            if (result == EditResult.Saved) mutableState.value = withContext(ioDispatcher) { load(mutableState.value.config) }
            result
        }

    private fun validateDocument(text: String): Pair<EditResult, String?> {
        if (text.toByteArray().size > ConfigDocuments.MAX_BYTES) {
            return EditResult.Refused(listOf(ConfigError("launcher.json", "supera el máximo de 1 MiB"))) to null
        }
        return when (val parsed = ConfigParser.parse(text, defaults)) {
            is ParseResult.Failure -> EditResult.Refused(parsed.errors) to null
            is ParseResult.Success -> EditResult.Saved to text
        }
    }

    private fun writeEdit(edit: BindingEdit): EditResult =
        try {
            val userText = source.readUserConfig()
            val current =
                when (val parsed = userText?.let { ConfigParser.parse(it, defaults) }) {
                    null -> defaults
                    is ParseResult.Success -> parsed.config
                    is ParseResult.Failure -> return EditResult.Refused(parsed.errors)
                }
            val bindings = if (edit == BindingEdit.ResetAll) defaults.bindings else BindingsEditor.apply(current.bindings, edit)
            val text = BindingsEditor.write(userText, defaults.bindings, bindings)
            when (val check = ConfigParser.parse(text, defaults)) {
                is ParseResult.Failure -> EditResult.Refused(check.errors)
                is ParseResult.Success -> EditResult.Saved.also { source.writeUserConfig(text) }
            }
        } catch (e: IOException) {
            EditResult.Refused(listOf(ConfigError("launcher.json", "no se pudo guardar: ${e.message}")))
        }

    /** La config de fábrica no cambia mientras la app corre: se lee y se parsea una vez. */
    private val defaults: LauncherConfig by lazy { parseDefaults() }

    private fun load(lastGood: LauncherConfig?): ConfigState {
        val keep = lastGood ?: defaults
        return try {
            when (val text = source.readUserConfig()) {
                null -> ConfigState(defaults)
                else ->
                    when (val result = ConfigParser.parse(text, fallback = defaults)) {
                        is ParseResult.Success -> ConfigState(result.config)
                        is ParseResult.Failure -> ConfigState(keep, result.errors)
                    }
            }
        } catch (e: IOException) {
            ConfigState(keep, listOf(ConfigError("launcher.json", "no se pudo leer: ${e.message}")))
        }
    }

    /** La config de fábrica es parte de la app: si no se lee, es un error de empaquetado, no del usuario. */
    private fun parseDefaults(): LauncherConfig =
        when (val result = ConfigParser.parse(source.readDefaultConfig())) {
            is ParseResult.Success -> result.config
            is ParseResult.Failure -> error("default-launcher.json is invalid: ${result.errors.joinToString()}")
        }
}
