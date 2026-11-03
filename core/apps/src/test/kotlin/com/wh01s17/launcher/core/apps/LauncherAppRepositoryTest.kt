package com.wh01s17.launcher.core.apps

import com.wh01s17.launcher.core.apps.model.AppCatalog
import com.wh01s17.launcher.core.apps.model.AppProfile
import com.wh01s17.launcher.core.apps.model.ProfileKind
import com.wh01s17.launcher.core.apps.source.AppEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * El repositorio vive en `backgroundScope`: `advanceUntilIdle()` no espera al trabajo de fondo,
 * así que cada paso usa `runCurrent()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LauncherAppRepositoryTest {
    private val personal = AppProfile(0, ProfileKind.Personal)
    private val work = AppProfile(10, ProfileKind.Work)

    private fun TestScope.repository(
        gateway: FakeLauncherAppsGateway,
        started: SharingStarted = SharingStarted.Eagerly,
    ) = LauncherAppRepository(gateway, backgroundScope, StandardTestDispatcher(testScheduler), started) { Locale.ROOT }

    private fun AppCatalog.labels() = apps.map { it.label }

    @Test
    fun `starts loading and then lists every profile`() =
        runTest {
            val gateway = FakeLauncherAppsGateway(listOf(personal, work), listOf(app("B", "b"), app("A", "a", userSerial = 10)))
            val repo = repository(gateway)
            assertFalse(repo.catalog.value.loaded)
            runCurrent()
            assertEquals(listOf("A", "B"), repo.catalog.value.labels())
            assertEquals(listOf(personal, work), repo.catalog.value.profiles)
        }

    @Test
    fun `a package event rereads only that package`() =
        runTest {
            val gateway = FakeLauncherAppsGateway(listOf(personal), listOf(app("A", "a"), app("B", "b")))
            val repo = repository(gateway)
            runCurrent()
            gateway.apps.removeAll { it.packageName == "a" }
            gateway.apps += app("C", "c")
            gateway.emit(AppEvent.PackagesChanged(0, setOf("a", "c")))
            runCurrent()
            assertEquals(listOf("B", "C"), repo.catalog.value.labels())
            assertEquals(1, gateway.fullReads)
            assertEquals(2, gateway.packageReads)
        }

    @Test
    fun `events are applied in order`() =
        runTest {
            val gateway = FakeLauncherAppsGateway(listOf(personal))
            val repo = repository(gateway)
            runCurrent()
            gateway.apps += app("A", "a")
            gateway.emit(AppEvent.PackagesChanged(0, setOf("a")))
            gateway.apps.clear()
            gateway.emit(AppEvent.PackagesChanged(0, setOf("a")))
            runCurrent()
            assertTrue(
                repo.catalog.value.apps
                    .isEmpty(),
            )
        }

    @Test
    fun `a profile change rereads everything`() =
        runTest {
            val gateway = FakeLauncherAppsGateway(listOf(personal), listOf(app("A", "a")))
            val repo = repository(gateway)
            runCurrent()
            gateway.profiles = listOf(personal, work.copy(available = false))
            gateway.apps += app("W", "w", userSerial = 10)
            gateway.emit(AppEvent.ProfilesChanged)
            runCurrent()
            assertEquals(listOf("A", "W"), repo.catalog.value.labels())
            assertFalse(
                repo.catalog.value
                    .profile(10)!!
                    .available,
            )
        }

    @Test
    fun `a package event for an unknown profile rereads everything`() =
        runTest {
            val gateway = FakeLauncherAppsGateway(listOf(personal))
            val repo = repository(gateway)
            runCurrent()
            gateway.profiles = listOf(personal, work)
            gateway.apps += app("W", "w", userSerial = 10)
            gateway.emit(AppEvent.PackagesChanged(10, setOf("w")))
            runCurrent()
            assertEquals(listOf("W"), repo.catalog.value.labels())
            assertEquals(2, gateway.fullReads + gateway.packageReads - 1)
        }

    @Test
    fun `a locale change rereads the names`() =
        runTest {
            val gateway = FakeLauncherAppsGateway(listOf(personal), listOf(app("Settings", "s")))
            val repo = repository(gateway)
            runCurrent()
            gateway.apps[0] = app("Ajustes", "s")
            gateway.emit(AppEvent.LocaleChanged)
            runCurrent()
            assertEquals(listOf("Ajustes"), repo.catalog.value.labels())
        }

    @Test
    fun `while subscribed unregisters the callback and reloads on return`() =
        runTest {
            val gateway = FakeLauncherAppsGateway(listOf(personal), listOf(app("A", "a")))
            val repo = repository(gateway, SharingStarted.WhileSubscribed(stopTimeoutMillis = 0))
            val first = backgroundScope.launch { repo.catalog.collect {} }
            runCurrent()
            assertEquals(1, gateway.subscribers)
            first.cancel()
            runCurrent()
            assertEquals(0, gateway.subscribers)
            // Un cambio sin nadie escuchando se pierde, pero al volver se relee todo.
            gateway.apps += app("B", "b")
            backgroundScope.launch { repo.catalog.collect {} }
            runCurrent()
            assertEquals(listOf("A", "B"), repo.catalog.value.labels())
        }
}
