package com.wh01s17.launcher.core.apps

import com.wh01s17.launcher.core.apps.model.AppKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppKeyTest {
    @Test
    fun `personal profile uses the short component form of the config`() {
        val key = AppKey("com.termux", "com.termux.HomeActivity", 0)
        assertEquals("com.termux/.HomeActivity", key.toConfigString(personalSerial = 0))
    }

    @Test
    fun `other profiles carry their serial`() {
        val key = AppKey("org.mozilla.firefox", "org.mozilla.gecko.BrowserApp", 10)
        assertEquals("org.mozilla.firefox/org.mozilla.gecko.BrowserApp@10", key.toConfigString(personalSerial = 0))
    }

    @Test
    fun `parse is the inverse of toConfigString`() {
        val keys =
            listOf(
                AppKey("com.termux", "com.termux.HomeActivity", 0),
                AppKey("com.termux", "com.termux.HomeActivity", 11),
                AppKey("a.b", "c.d.E", 0),
                AppKey("pkg", "pkg.Main", 7),
            )
        for (key in keys) assertEquals(key, AppKey.parse(key.toConfigString(personalSerial = 0), personalSerial = 0))
    }

    @Test
    fun `parse without serial uses the personal profile`() {
        assertEquals(AppKey("com.termux", "com.termux.HomeActivity", 3), AppKey.parse("com.termux/.HomeActivity", personalSerial = 3))
    }

    @Test
    fun `parse rejects malformed values`() {
        for (value in listOf("", "com.termux", "/.Home", "com.termux/", "com.termux/.Home@x")) {
            assertNull(value, AppKey.parse(value, personalSerial = 0))
        }
    }
}
