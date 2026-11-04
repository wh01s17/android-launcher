package com.wh01s17.launcher.core.config

import com.wh01s17.launcher.core.input.WorkspaceCommand
import com.wh01s17.launcher.core.input.binding.BindingTrigger
import com.wh01s17.launcher.core.input.binding.ChordParse
import com.wh01s17.launcher.core.input.binding.KeyChord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BindingsEditorTest {
    private class MemorySource(
        var user: String? = null,
    ) : ConfigSource {
        var writes = 0

        override fun readUserConfig() = user

        override fun readDefaultConfig() = File("src/main/assets/default-launcher.json").readText()

        override fun writeUserConfig(text: String) {
            user = text
            writes++
        }
    }

    private fun keys(text: String) = BindingTrigger.Keys((KeyChord.parse(text) as ChordParse.Valid).chord)

    private fun repository(source: MemorySource) = ConfigRepository(source, Dispatchers.Unconfined).also { runBlocking { it.reload() } }

    private fun command(
        repository: ConfigRepository,
        text: String,
    ) = repository.state.value.config!!
        .bindings
        .firstOrNull { it.trigger == keys(text) }
        ?.command

    private fun userBindings(source: MemorySource) =
        (Json.parseToJsonElement(source.user!!) as JsonObject)["bindings"]?.jsonArray?.map {
            it.toString()
        }

    @Test
    fun `rebinding writes only the difference from the factory and keeps the rest of the file`() =
        runBlocking {
            val source = MemorySource("""{"version":1,"general":{"layout":"master"}}""")
            val repository = repository(source)
            assertEquals(EditResult.Saved, repository.editBindings(BindingEdit.Rebind(keys("SUPER + W"), keys("SUPER + Q"))))
            assertEquals(WorkspaceCommand.CloseTile, command(repository, "SUPER + Q"))
            assertNull(command(repository, "SUPER + W"))
            assertEquals(
                listOf(
                    """{"keys":"SUPER + Q","command":"tile.close"}""",
                    """{"keys":"SUPER + W","command":"none"}""",
                ),
                userBindings(source),
            )
            assertTrue(source.user!!.contains("\"master\""))
        }

    @Test
    fun `taking a chord from another action removes it there, and arguments are written`() =
        runBlocking {
            val source = MemorySource()
            val repository = repository(source)
            repository.editBindings(BindingEdit.Rebind(keys("SUPER + 2"), keys("SUPER + 1")))
            assertEquals(WorkspaceCommand.FocusWorkspace(2), command(repository, "SUPER + 1"))
            assertNull(command(repository, "SUPER + 2"))
            assertEquals(
                listOf(
                    """{"keys":"SUPER + 1","command":"workspace.focus","args":{"id":2}}""",
                    """{"keys":"SUPER + 2","command":"none"}""",
                ),
                userBindings(source),
            )
        }

    @Test
    fun `removing and resetting all go back and forth`() =
        runBlocking {
            val source = MemorySource()
            val repository = repository(source)
            repository.editBindings(BindingEdit.Remove(keys("SUPER + T")))
            assertNull(command(repository, "SUPER + T"))
            repository.editBindings(BindingEdit.ResetAll)
            assertEquals(WorkspaceCommand.ToggleFloating, command(repository, "SUPER + T"))
            assertNull(userBindings(source))
        }

    @Test
    fun `a broken user file is never overwritten`() =
        runBlocking {
            val source = MemorySource("""{"version":1,"bindings":[{"keys":"HYPER + 1","command":"menu.open"}]}""")
            val repository = repository(source)
            val result = repository.editBindings(BindingEdit.Remove(keys("SUPER + T")))
            assertEquals(listOf("bindings[0].keys"), (result as EditResult.Refused).errors.map { it.path })
            assertEquals(0, source.writes)
        }
}
