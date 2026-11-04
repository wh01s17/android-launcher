package com.wh01s17.launcher.core.config

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException

/** De dónde salen los textos de config; un doble los sustituye en las pruebas. */
interface ConfigSource {
    /** El `launcher.json` del usuario, o `null` si no existe. Bloqueante. */
    fun readUserConfig(): String?

    /** La config de fábrica, empaquetada en la app. Bloqueante. */
    fun readDefaultConfig(): String
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
    suspend fun reload() {
        val next = withContext(ioDispatcher) { load(mutableState.value.config) }
        mutableState.value = next
    }

    private fun load(lastGood: LauncherConfig?): ConfigState {
        val defaults = parseDefaults()
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
