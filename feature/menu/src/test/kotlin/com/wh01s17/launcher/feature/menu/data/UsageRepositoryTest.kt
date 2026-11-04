package com.wh01s17.launcher.feature.menu.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UsageRepositoryTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun `counts and timestamps survive closing and recreating datastore`() =
        runTest {
            val file = File(temporary.root, "usage.preferences_pb")
            val job = SupervisorJob()
            val scope = CoroutineScope(job + StandardTestDispatcher(testScheduler))
            val first = DataStoreUsageRepository.create(file, scope)
            first.record("app:profile10/pkg/Activity")
            first.record("app:profile10/pkg/Activity")
            assertEquals(
                2,
                first.usage
                    .first()
                    .values
                    .single()
                    .launchCount,
            )
            scope.cancel()
            job.join()
            val second = DataStoreUsageRepository.create(file, backgroundScope)
            val saved = second.usage.first()
            assertEquals(2, saved.values.single().launchCount)
            assertTrue(saved.values.single().lastUsedAtMillis > 0)
        }

    @Test
    fun `corrupt datastore resets only ephemeral usage`() =
        runTest {
            val file = File(temporary.root, "corrupt.preferences_pb")
            file.writeText("invalid protobuf")
            val repository = DataStoreUsageRepository.create(file, backgroundScope)
            assertTrue(repository.usage.first().isEmpty())
            repository.record("command:Wifi")
            assertEquals(
                1,
                repository.usage
                    .first()
                    .values
                    .single()
                    .launchCount,
            )
        }

    @Test
    fun `unknown history schema resets without mixing incompatible counts`() =
        runTest {
            val file = File(temporary.root, "future.preferences_pb")
            val store = PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })
            store.edit { preferences ->
                preferences[intPreferencesKey("schema")] = 99
                preferences[stringPreferencesKey("usage:old")] = "100,1000"
            }
            val repository = DataStoreUsageRepository(store)
            assertTrue(repository.usage.first().isEmpty())
            repository.record("command:Wifi")
            assertEquals(setOf("command:Wifi"), repository.usage.first().keys)
        }
}
