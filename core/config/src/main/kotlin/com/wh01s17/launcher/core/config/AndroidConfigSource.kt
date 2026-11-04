package com.wh01s17.launcher.core.config

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.IOException

/**
 * `files/config/launcher.json` (exportable e importable por el usuario) y
 * `assets/default-launcher.json` (plan.md §6.1 y §6.3).
 */
class AndroidConfigSource(
    private val context: Context,
) : ConfigSource {
    private val userFile get() = File(File(context.filesDir, DIRECTORY), FILE_NAME)

    override fun readUserConfig(): String? = userFile.takeIf { it.isFile }?.readText()

    override fun writeUserConfig(text: String) {
        val file = userFile
        file.parentFile?.mkdirs()
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        try {
            stream.write(text.toByteArray())
            atomic.finishWrite(stream)
        } catch (e: IOException) {
            atomic.failWrite(stream)
            throw e
        }
    }

    override fun readDefaultConfig(): String =
        context.assets
            .open(DEFAULT_ASSET)
            .bufferedReader()
            .use { it.readText() }

    private companion object {
        const val DIRECTORY = "config"
        const val FILE_NAME = "launcher.json"
        const val DEFAULT_ASSET = "default-launcher.json"
    }
}
