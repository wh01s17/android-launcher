package com.wh01s17.launcher.buildlogic.release

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ReleaseConfigTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `versionCode follows plan section 8_1`() {
        assertEquals(100, AppVersion.parse("0.1.0").code)
        assertEquals(500, AppVersion.parse("0.5.0").code)
        assertEquals(10_000, AppVersion.parse("1.0.0").code)
        assertEquals(10_203, AppVersion.parse(" 1.2.3 ").code)
        assertEquals("1.2.3", AppVersion.parse("1.2.3").name)
    }

    @Test
    fun `rejects versions that would collide in versionCode`() {
        assertThrows(IllegalArgumentException::class.java) { AppVersion.parse("1.100.0") }
        assertThrows(IllegalArgumentException::class.java) { AppVersion.parse("1.0.100") }
        assertThrows(IllegalStateException::class.java) { AppVersion.parse("1.0") }
        assertThrows(IllegalStateException::class.java) { AppVersion.parse("1.0.0-beta") }
    }

    @Test
    fun `reads the version key`() {
        val file = File(dir, "version.properties").apply { writeText("# comment\nversion=1.0.0\n") }
        assertEquals(AppVersion(1, 0, 0), AppVersion.read(file))
    }

    @Test
    fun `no signing data means an unsigned release`() {
        assertNull(ReleaseSigning.resolve(env = { null }, propertiesFile = File(dir, "keystore.properties")))
    }

    @Test
    fun `environment wins over keystore properties`() {
        val store = File(dir, "ci.jks").apply { writeText("x") }
        val env =
            mapOf(
                ReleaseSigning.ENV_STORE_FILE to store.path,
                ReleaseSigning.ENV_STORE_PASSWORD to "a",
                ReleaseSigning.ENV_KEY_ALIAS to "b",
                ReleaseSigning.ENV_KEY_PASSWORD to "c",
            )
        File(dir, "keystore.properties").writeText("storeFile=missing.jks\n")
        val signing = ReleaseSigning.resolve(env = env::get, propertiesFile = File(dir, "keystore.properties"))
        assertEquals(ReleaseSigning(store, "a", "b", "c"), signing)
    }

    @Test
    fun `keystore properties resolve the store next to the file`() {
        File(dir, "local.jks").writeText("x")
        val props =
            File(dir, "keystore.properties").apply {
                writeText("storeFile=local.jks\nstorePassword=p\nkeyAlias=k\nkeyPassword=q\n")
            }
        val signing = ReleaseSigning.resolve(env = { null }, propertiesFile = props)
        assertEquals(File(dir, "local.jks").canonicalPath, signing?.storeFile?.canonicalPath)
        assertEquals("k", signing?.keyAlias)
    }

    @Test
    fun `partial signing data fails instead of shipping unsigned`() {
        val env = mapOf(ReleaseSigning.ENV_STORE_PASSWORD to "a")
        assertThrows(IllegalStateException::class.java) {
            ReleaseSigning.resolve(env = env::get, propertiesFile = File(dir, "keystore.properties"))
        }
    }
}
