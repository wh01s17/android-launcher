package com.wh01s17.launcher.core.apps

import com.wh01s17.launcher.core.apps.model.AppCatalog
import com.wh01s17.launcher.core.apps.model.AppProfile
import com.wh01s17.launcher.core.apps.model.ProfileKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AppCatalogTest {
    private val es = Locale.forLanguageTag("es")
    private val profiles = listOf(AppProfile(0, ProfileKind.Personal), AppProfile(10, ProfileKind.Work))

    @Test
    fun `apps are sorted by name ignoring case and accents`() {
        val catalog =
            AppCatalog.of(profiles, listOf(app("zoom", "z"), app("Ábaco", "a2"), app("abeja", "a1"), app("Música", "m")), es)
        assertEquals(listOf("Ábaco", "abeja", "Música", "zoom"), catalog.apps.map { it.label })
        assertTrue(catalog.loaded)
    }

    @Test
    fun `same name puts the personal profile first`() {
        val catalog = AppCatalog.of(profiles, listOf(app("Chrome", "c", userSerial = 10), app("Chrome", "c", userSerial = 0)), es)
        assertEquals(listOf(0L, 10L), catalog.apps.map { it.key.userSerial })
    }

    @Test
    fun `duplicate keys are listed once`() {
        val catalog = AppCatalog.of(profiles, listOf(app("A", "a"), app("A", "a")), es)
        assertEquals(1, catalog.apps.size)
    }

    @Test
    fun `replacePackages only touches that package in that profile`() {
        val catalog =
            AppCatalog.of(profiles, listOf(app("Old", "p"), app("Old", "p", userSerial = 10), app("Keep", "k")), es)
        val updated = catalog.replacePackages(0, setOf("p"), listOf(app("New", "p")), es)
        assertEquals(listOf("Keep" to 0L, "New" to 0L, "Old" to 10L), updated.apps.map { it.label to it.key.userSerial })
    }

    @Test
    fun `removing a package leaves no entry`() {
        val catalog = AppCatalog.of(profiles, listOf(app("Gone", "g"), app("Keep", "k")), es)
        assertEquals(listOf("Keep"), catalog.replacePackages(0, setOf("g"), emptyList(), es).apps.map { it.label })
    }

    @Test
    fun `loading has no profiles and is not loaded`() {
        assertFalse(AppCatalog.Loading.loaded)
        assertEquals(null, AppCatalog.Loading.personalSerial)
        assertEquals(0L, AppCatalog.of(profiles, emptyList()).personalSerial)
    }

    @Test
    fun `find takes a package or an activity, preferring the personal profile`() {
        val catalog =
            AppCatalog.of(
                profiles,
                listOf(
                    app("Termux", "com.termux", userSerial = 10, className = "com.termux.app.TermuxActivity"),
                    app("Termux", "com.termux", className = "com.termux.app.TermuxActivity"),
                ),
                es,
            )
        assertEquals(0L, catalog.find("com.termux")?.key?.userSerial)
        assertEquals(0L, catalog.find("com.termux/.app.TermuxActivity")?.key?.userSerial)
        assertEquals(10L, catalog.find("com.termux/.app.TermuxActivity@10")?.key?.userSerial)
        assertEquals(null, catalog.find("com.termux/.HomeActivity"))
        assertEquals(null, catalog.find("org.missing"))
    }
}
