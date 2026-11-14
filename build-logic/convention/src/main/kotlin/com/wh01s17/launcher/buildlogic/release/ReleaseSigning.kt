package com.wh01s17.launcher.buildlogic.release

import java.io.File
import java.util.Properties

/**
 * Datos de la clave de firma de release (plan.md §8.2). Nunca se versionan: salen de las variables de
 * entorno de CI o, en local, de `keystore.properties` (ignorado por git). Ver `docs/release.md`.
 */
data class ReleaseSigning(
    val storeFile: File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
) {
    companion object {
        const val ENV_STORE_FILE = "SIGNING_KEYSTORE_FILE"
        const val ENV_STORE_PASSWORD = "SIGNING_STORE_PASSWORD"
        const val ENV_KEY_ALIAS = "SIGNING_KEY_ALIAS"
        const val ENV_KEY_PASSWORD = "SIGNING_KEY_PASSWORD"

        /**
         * Las variables de entorno ganan a `keystore.properties`. Sin datos completos devuelve `null`
         * y la release sale sin firmar; con datos a medias falla, para no publicar algo sin firma
         * creyendo que la lleva.
         */
        fun resolve(
            env: (String) -> String?,
            propertiesFile: File,
        ): ReleaseSigning? {
            val fromEnv = fromValues(env(ENV_STORE_FILE), env(ENV_STORE_PASSWORD), env(ENV_KEY_ALIAS), env(ENV_KEY_PASSWORD))
            if (fromEnv != null || !propertiesFile.isFile) return fromEnv
            val properties = Properties().apply { propertiesFile.reader().use(::load) }
            val base = propertiesFile.absoluteFile.parentFile
            return fromValues(
                properties.getProperty("storeFile")?.let { base.resolve(it).path },
                properties.getProperty("storePassword"),
                properties.getProperty("keyAlias"),
                properties.getProperty("keyPassword"),
            ) ?: error("${propertiesFile.path} needs storeFile, storePassword, keyAlias and keyPassword")
        }

        private fun fromValues(vararg values: String?): ReleaseSigning? {
            val present = values.map { !it.isNullOrBlank() }
            if (present.none { it }) return null
            check(present.all { it }) { "incomplete release signing: set all of $ENV_STORE_FILE, $ENV_STORE_PASSWORD, ..." }
            val (file, storePassword, alias, keyPassword) = values.map { it!! }
            val store = File(file)
            check(store.isFile) { "release keystore not found: ${store.path}" }
            return ReleaseSigning(store, storePassword, alias, keyPassword)
        }
    }
}
