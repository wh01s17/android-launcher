package com.wh01s17.launcher.feature.workspaces.data

import com.wh01s17.launcher.core.apps.model.AppKey
import com.wh01s17.launcher.core.config.GeneralConfig
import com.wh01s17.launcher.core.tiling.TilingLayout
import com.wh01s17.launcher.feature.workspaces.model.Desk
import com.wh01s17.launcher.feature.workspaces.model.DeskCodec
import com.wh01s17.launcher.feature.workspaces.model.TileContent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DeskRepositoryTest {
    private val dispatcher = StandardTestDispatcher()
    private val termux = TileContent.App(AppKey("com.termux", "com.termux.HomeActivity", 0))

    private class FakeStorage(
        var text: String? = null,
    ) : DeskStorage {
        val writes = mutableListOf<String>()
        var brokenKept = false
        var failWrites = false

        override fun read() = text

        override fun write(text: String) {
            if (failWrites) throw IOException("disk full")
            writes += text
            this.text = text
        }

        override fun keepBroken() {
            brokenKept = true
        }
    }

    private fun TestScope.repository(storage: FakeStorage) = DeskRepository(storage, backgroundScope, dispatcher, saveDelayMillis = DELAY)

    @Test
    fun `starts empty without a file and ignores changes until it has read`() =
        runTest(dispatcher) {
            val repo = repository(FakeStorage())
            assertNull(repo.state.value)
            repo.update { it.add(termux) }
            runCurrent()
            assertEquals(Desk(), repo.state.value)
        }

    @Test
    fun `reads what was saved`() =
        runTest(dispatcher) {
            val desk = Desk().add(termux).focusWorkspace(3)
            val repo = repository(FakeStorage(DeskCodec.encode(desk).toString()))
            runCurrent()
            assertEquals(desk, repo.state.value)
        }

    @Test
    fun `a broken file is kept aside and the desk starts over`() =
        runTest(dispatcher) {
            for (text in listOf("{not json", """{"version":1}""")) {
                val storage = FakeStorage(text)
                val repo = repository(storage)
                runCurrent()
                assertEquals(Desk(), repo.state.value)
                assertTrue(storage.brokenKept)
            }
        }

    @Test
    fun `quick changes are saved once, after the delay`() =
        runTest(dispatcher) {
            val storage = FakeStorage()
            val repo = repository(storage)
            runCurrent()
            repo.update { it.add(termux) }
            runCurrent()
            repo.update { it.focusWorkspace(2) }
            advanceTimeBy(DELAY - 1)
            runCurrent()
            assertEquals(0, storage.writes.size)
            advanceTimeBy(2)
            runCurrent()
            assertEquals(listOf(DeskCodec.encode(Desk().add(termux).focusWorkspace(2)).toString()), storage.writes)
        }

    @Test
    fun `flush saves right away and never writes the same desk twice`() =
        runTest(dispatcher) {
            val storage = FakeStorage()
            val repo = repository(storage)
            runCurrent()
            // Lo leído no se vuelve a escribir.
            repo.flush()
            assertEquals(0, storage.writes.size)
            repo.update { it.add(termux) }
            repo.flush()
            assertEquals(1, storage.writes.size)
            advanceTimeBy(DELAY * 2)
            runCurrent()
            assertEquals(1, storage.writes.size)
        }

    @Test
    fun `a failed write is retried by the next flush`() =
        runTest(dispatcher) {
            val storage = FakeStorage().apply { failWrites = true }
            val repo = repository(storage)
            runCurrent()
            repo.update { it.add(termux) }
            repo.flush()
            assertEquals(0, storage.writes.size)
            storage.failWrites = false
            repo.flush()
            assertEquals(1, storage.writes.size)
        }

    @Test
    fun `defaults apply before and after reading`() =
        runTest(dispatcher) {
            val repo = repository(FakeStorage())
            repo.applyDefaults(GeneralConfig(layout = TilingLayout.Master, maxTilesPerWorkspace = 3))
            runCurrent()
            assertEquals(
                TilingLayout.Master,
                repo.state.value!!
                    .workspace(1)
                    .layout,
            )
            repo.applyDefaults(GeneralConfig(maxTilesPerWorkspace = 5))
            assertEquals(
                5,
                repo.state.value!!
                    .workspace(10)
                    .maxTiles,
            )
            assertEquals(
                TilingLayout.Dwindle,
                repo.state.value!!
                    .workspace(10)
                    .layout,
            )
        }

    private companion object {
        const val DELAY = 500L
    }
}
