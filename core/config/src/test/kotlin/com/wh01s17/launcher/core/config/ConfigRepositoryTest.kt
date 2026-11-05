package com.wh01s17.launcher.core.config

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigRepositoryTest {
    private class FakeSource(
        var user: String? = null,
        var failToRead: Boolean = false,
        val default: String = File("src/main/assets/default-launcher.json").readText(),
    ) : ConfigSource {
        override fun readUserConfig(): String? = if (failToRead) throw IOException("disk") else user

        override fun readDefaultConfig() = default

        override fun writeUserConfig(text: String) {
            user = text
        }
    }

    private val source = FakeSource()

    private fun kotlinx.coroutines.test.TestScope.repository() = ConfigRepository(source, StandardTestDispatcher(testScheduler))

    @Test
    fun `nothing is loaded before the first reload`() =
        runTest {
            assertNull(repository().state.value.config)
        }

    @Test
    fun `without a user file the factory config is used`() =
        runTest {
            val repo = repository()
            repo.reload()
            assertEquals(
                FACTORY_BAR_MODULES,
                repo.state.value.config!!
                    .bar.layout.all.size,
            )
            assertTrue(
                repo.state.value.errors
                    .isEmpty(),
            )
        }

    @Test
    fun `a valid user config replaces the factory one`() =
        runTest {
            val repo = repository()
            source.user = """{"version":1,"bar":{"layout":{"left":[{"id":"core.menu"}],"center":[],"right":[]}}}"""
            repo.reload()
            assertEquals(
                listOf("core.menu"),
                repo.state.value.config!!
                    .bar.layout.all
                    .map { it.id },
            )
        }

    @Test
    fun `an invalid user config keeps the last good one and reports the errors`() =
        runTest {
            val repo = repository()
            source.user = """{"version":1,"bar":{"layout":{"left":[{"id":"core.menu"}],"center":[],"right":[]}}}"""
            repo.reload()
            source.user = """{"version":1,"bar":{"outline":"si"}}"""
            repo.reload()
            assertEquals(
                listOf("core.menu"),
                repo.state.value.config!!
                    .bar.layout.all
                    .map { it.id },
            )
            assertEquals(
                listOf("bar.outline"),
                repo.state.value.errors
                    .map { it.path },
            )
            // Al arreglarla, los errores desaparecen.
            source.user = """{"version":1,"bar":{"outline":true}}"""
            repo.reload()
            assertTrue(
                repo.state.value.errors
                    .isEmpty(),
            )
            assertEquals(
                true,
                repo.state.value.config!!
                    .bar.outline,
            )
        }

    @Test
    fun `an invalid first config falls back to the factory one`() =
        runTest {
            val repo = repository()
            source.user = "not json"
            repo.reload()
            assertEquals(
                FACTORY_BAR_MODULES,
                repo.state.value.config!!
                    .bar.layout.all.size,
            )
            assertEquals(1, repo.state.value.errors.size)
        }

    @Test
    fun `an unreadable file is an error and not a crash`() =
        runTest {
            val repo = repository()
            source.failToRead = true
            repo.reload()
            assertEquals(
                "launcher.json",
                repo.state.value.errors
                    .single()
                    .path,
            )
            assertTrue(repo.state.value.config != null)
        }

    @Test
    fun `the factory config is read once and reloads run one after another`() =
        runTest {
            var defaultReads = 0
            val counting =
                object : ConfigSource {
                    override fun readUserConfig(): String? = source.readUserConfig()

                    override fun readDefaultConfig() = source.default.also { defaultReads++ }

                    override fun writeUserConfig(text: String) = source.writeUserConfig(text)
                }
            val repo = ConfigRepository(counting, StandardTestDispatcher(testScheduler))
            val jobs = List(3) { backgroundScope.launch { repo.reload() } }
            jobs.forEach { it.join() }
            repo.reload()
            assertEquals(1, defaultReads)
            assertEquals(
                FACTORY_BAR_MODULES,
                repo.state.value.config!!
                    .bar.layout.all.size,
            )
        }

    @Test
    fun `deleting the user file goes back to the factory config`() =
        runTest {
            val repo = repository()
            source.user = """{"version":1,"bar":{"outline":true}}"""
            repo.reload()
            source.user = null
            repo.reload()
            assertEquals(
                false,
                repo.state.value.config!!
                    .bar.outline,
            )
        }

    private companion object {
        /** Módulos de la barra en `default-launcher.json`: así se reconoce que se usó la de fábrica. */
        const val FACTORY_BAR_MODULES = 7
    }
}
