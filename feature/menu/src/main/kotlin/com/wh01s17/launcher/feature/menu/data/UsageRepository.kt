package com.wh01s17.launcher.feature.menu.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wh01s17.launcher.core.fuzzy.Usage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException

interface UsageRepository {
    val usage: Flow<Map<String, Usage>>

    suspend fun record(id: String)
}

/** Historial vacío para pruebas o para desactivar el ranking. */
object EmptyUsageRepository : UsageRepository {
    override val usage = flowOf(emptyMap<String, Usage>())

    override suspend fun record(id: String) = Unit
}

/** Una instancia por archivo en AppContainer; DataStore serializa las escrituras. */
class DataStoreUsageRepository(
    private val store: DataStore<Preferences>,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : UsageRepository {
    override val usage =
        store.data
            .catch { error ->
                if (error is IOException) emit(emptyPreferences()) else throw error
            }.map { preferences ->
                if (preferences[VERSION] != SCHEMA_VERSION) return@map emptyMap()
                preferences
                    .asMap()
                    .mapNotNull { (key, value) ->
                        if (!key.name.startsWith(PREFIX) || value !is String) return@mapNotNull null
                        parseUsage(value)?.let { key.name.removePrefix(PREFIX) to it }
                    }.toMap()
            }

    override suspend fun record(id: String) {
        store.edit { preferences ->
            if (preferences[VERSION] != SCHEMA_VERSION) preferences.clear()
            preferences[VERSION] = SCHEMA_VERSION
            val key = stringPreferencesKey(PREFIX + id)
            val old = preferences[key]?.let(::parseUsage)
            val count = ((old?.launchCount ?: 0).toLong() + 1).coerceAtMost(Int.MAX_VALUE.toLong())
            preferences[key] = "$count,${nowMillis()}"
        }
    }

    companion object {
        private const val SCHEMA_VERSION = 1
        private const val PREFIX = "usage:"
        private val VERSION = intPreferencesKey("schema")

        fun create(
            file: File,
            scope: CoroutineScope,
        ): DataStoreUsageRepository =
            DataStoreUsageRepository(
                PreferenceDataStoreFactory.create(
                    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                    scope = scope,
                    produceFile = { file },
                ),
            )

        private fun parseUsage(value: String): Usage? {
            val parts = value.split(',')
            if (parts.size != 2) return null
            val count = parts[0].toIntOrNull()?.takeIf { it >= 0 } ?: return null
            val time = parts[1].toLongOrNull()?.takeIf { it >= 0 } ?: return null
            return Usage(count, time)
        }
    }
}
