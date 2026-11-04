package com.wh01s17.launcher.feature.workspaces.data

import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/** Dónde se guarda el [com.wh01s17.launcher.feature.workspaces.model.Desk]; un doble lo sustituye en las pruebas. */
interface DeskStorage {
    /** El texto guardado, o `null` si nunca se guardó. Bloqueante. */
    fun read(): String?

    /** Bloqueante. Lanza [IOException] si no se pudo escribir; lo anterior sigue intacto. */
    fun write(text: String)

    /** Aparta una copia del archivo actual que no se pudo leer, para no perderlo al escribir encima. */
    fun keepBroken()
}

/**
 * `files/state/workspaces.json`. [AtomicFile] escribe en un temporal y lo renombra: un corte de
 * luz o un cierre a mitad deja el archivo anterior, nunca uno a medias.
 */
class FileDeskStorage(
    private val file: File,
) : DeskStorage {
    private val atomic = AtomicFile(file)

    override fun read(): String? =
        try {
            atomic.readFully().decodeToString()
        } catch (ignored: FileNotFoundException) {
            null
        }

    override fun write(text: String) {
        file.parentFile?.mkdirs()
        val out = atomic.startWrite()
        try {
            out.write(text.encodeToByteArray())
            atomic.finishWrite(out)
        } catch (e: IOException) {
            atomic.failWrite(out)
            throw e
        }
    }

    override fun keepBroken() {
        if (file.isFile) file.copyTo(File(file.parentFile, "${file.name}.broken"), overwrite = true)
    }
}
