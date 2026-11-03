package com.wh01s17.launcher.core.system.oem.xiaomi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NavigationFilesTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `defaults when nothing was written`() {
        val files = NavigationFiles(folder.root)
        assertEquals(GestureModePrefs(), files.readPrefs())
        assertNull(files.readStatus())
    }

    @Test
    fun `prefs and status round trip`() {
        val files = NavigationFiles(File(folder.root, "nested"))
        val prefs = GestureModePrefs(enabled = true, hideGestureLine = false, previousGestureLine = 1)
        files.writePrefs(prefs)
        files.writeStatus(NavigationStatus(GestureModeStatus.GaveUp, 1234))
        // Otro "proceso": otra instancia sobre el mismo directorio.
        val other = NavigationFiles(File(folder.root, "nested"))
        assertEquals(prefs, other.readPrefs())
        assertEquals(NavigationStatus(GestureModeStatus.GaveUp, 1234), other.readStatus())
        assertEquals(listOf("navigation-mode.properties", "navigation-status.properties"), File(folder.root, "nested").list()!!.sorted())
    }

    @Test
    fun `a corrupt or unknown status reads as missing`() {
        val files = NavigationFiles(folder.root)
        File(folder.root, "navigation-status.properties").writeText("status=Nope\n")
        assertNull(files.readStatus())
        File(folder.root, "navigation-mode.properties").writeText("enabled=\\u12")
        assertEquals(GestureModePrefs(), files.readPrefs())
    }
}
