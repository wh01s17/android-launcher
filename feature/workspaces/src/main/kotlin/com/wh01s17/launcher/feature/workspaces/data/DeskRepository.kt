package com.wh01s17.launcher.feature.workspaces.data

import com.wh01s17.launcher.core.config.GeneralConfig
import com.wh01s17.launcher.feature.workspaces.model.Desk
import com.wh01s17.launcher.feature.workspaces.model.DeskCodec
import com.wh01s17.launcher.feature.workspaces.model.withDefaults
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Los escritorios del launcher, vivos lo mismo que el proceso (plan.md §7 Hito 4: "persisten tras
 * reiniciar el teléfono"). Se leen una vez al crearlo y se guardan [saveDelayMillis] después del
 * último cambio, o al momento con [flush] (al salir del inicio).
 *
 * [state] es `null` hasta terminar la lectura; los cambios que lleguen antes se ignoran. Un archivo
 * que no se entiende se aparta (`.broken`) y se empieza de cero: nunca deja el launcher sin abrir.
 */
@OptIn(FlowPreview::class)
class DeskRepository(
    private val storage: DeskStorage,
    scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val saveDelayMillis: Long = SAVE_DELAY_MILLIS,
) {
    private val mutableState = MutableStateFlow<Desk?>(null)
    val state: StateFlow<Desk?> = mutableState.asStateFlow()

    private val writeLock = Mutex()

    /** Lo último que está en disco (protegido por [writeLock]). */
    private var saved: Desk? = null

    private var defaults: GeneralConfig = GeneralConfig()

    init {
        scope.launch {
            val loaded = withContext(ioDispatcher) { load() }.withDefaults(defaults.layout, defaults.maxTilesPerWorkspace)
            writeLock.withLock { saved = loaded }
            mutableState.value = loaded
            mutableState.filterNotNull().debounce(saveDelayMillis).collect(::save)
        }
    }

    fun update(transform: (Desk) -> Desk) = mutableState.update { it?.let(transform) }

    /** `general` de la config: el máximo de tiles y el layout de los workspaces vacíos. */
    fun applyDefaults(general: GeneralConfig) {
        defaults = general
        update { it.withDefaults(general.layout, general.maxTilesPerWorkspace) }
    }

    /** Guarda ya lo pendiente (p. ej. al salir del inicio, por si Android mata el proceso). */
    suspend fun flush() {
        mutableState.value?.let { save(it) }
    }

    private suspend fun save(desk: Desk) =
        writeLock.withLock {
            if (desk == saved) return@withLock
            try {
                withContext(ioDispatcher) { storage.write(DeskCodec.encode(desk).toString()) }
                saved = desk
            } catch (ignored: IOException) {
                // Disco lleno o similar: el estado sigue en memoria y se reintenta con el próximo
                // cambio o el próximo `flush`.
            }
        }

    private fun load(): Desk {
        val text =
            try {
                storage.read()
            } catch (ignored: IOException) {
                null
            } ?: return Desk()
        val element =
            try {
                Json.parseToJsonElement(text)
            } catch (ignored: SerializationException) {
                null
            }
        val result = element?.let(DeskCodec::decode)
        if (result is DeskCodec.DecodeResult.Success) return result.desk
        storage.keepBroken()
        return Desk()
    }

    private companion object {
        const val SAVE_DELAY_MILLIS = 500L
    }
}
