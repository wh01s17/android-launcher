package com.wh01s17.launcher.core.system.oem.xiaomi

import com.wh01s17.launcher.core.system.navigation.EdgeSensitivity
import com.wh01s17.launcher.core.system.navigation.EdgeSettings
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

    @Test
    fun `edge settings round trip and odd values fall back`() {
        val files = NavigationFiles(folder.root)
        assertEquals(EdgeSettings(), files.readEdges())
        val edges =
            EdgeSettings(
                backLeft = false,
                recents = false,
                sideThicknessDp = 20,
                bottomThicknessDp = 10,
                sensitivity = EdgeSensitivity.High,
            )
        files.writeEdges(edges)
        assertEquals(edges, files.readEdges())
        File(folder.root, "navigation-edges.properties").writeText("home=maybe\nsensitivity=Turbo\nsideThicknessDp=x\n")
        assertEquals(EdgeSettings(), files.readEdges())
    }
}
