package com.wh01s17.launcher.buildlogic.release

import java.io.File
import java.util.Properties

/**
 * Versión de la app leída de `version.properties`, su única fuente (plan.md §8.1).
 *
 * `versionCode = MAJOR * 10000 + MINOR * 100 + PATCH`, así que `MINOR` y `PATCH` van de 0 a 99.
 */
data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) {
    init {
        require(major in 0..MAX_MAJOR) { "MAJOR must be 0..$MAX_MAJOR: $major" }
        require(minor in 0..MAX_PART) { "MINOR must be 0..$MAX_PART: $minor" }
        require(patch in 0..MAX_PART) { "PATCH must be 0..$MAX_PART: $patch" }
    }

    val name: String get() = "$major.$minor.$patch"

    val code: Int get() = major * MAJOR_STEP + minor * MINOR_STEP + patch

    companion object {
        private const val MAJOR_STEP = 10_000
        private const val MINOR_STEP = 100
        private const val MAX_PART = 99

        // versionCode no puede pasar de 2 100 000 000 (Play), con margen.
        private const val MAX_MAJOR = 2_000
        private val SEMVER = Regex("""(\d+)\.(\d+)\.(\d+)""")

        fun parse(text: String): AppVersion {
            val match = SEMVER.matchEntire(text.trim()) ?: error("version must be MAJOR.MINOR.PATCH: '$text'")
            val (major, minor, patch) = match.destructured
            return AppVersion(major.toInt(), minor.toInt(), patch.toInt())
        }

        /** Lee la clave `version` del archivo de propiedades. */
        fun read(file: File): AppVersion {
            require(file.isFile) { "missing ${file.path} (plan.md §8.1)" }
            val properties = Properties().apply { file.reader().use(::load) }
            val value = properties.getProperty("version") ?: error("${file.path} needs a 'version' key")
            return parse(value)
        }
    }
}
