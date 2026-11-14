package com.wh01s17.launcher.core.apps.source

import java.io.File

/**
 * Nombres de las apps guardados entre arranques (plan.md §7 Hito 11). Leer el nombre de una actividad
 * obliga a Android a abrir los recursos de su APK: con 156 apps era casi toda la CPU de los tres
 * primeros segundos de un arranque en frío (simpleperf en el Redmi), y los tiles esperaban vacíos.
 *
 * La clave incluye la ruta del APK (cambia en cada instalación o actualización) y el idioma, así que
 * un nombre nunca queda viejo: si algo cambia, la clave es otra y se vuelve a leer. Una caché ilegible
 * se trata como vacía.
 */
class LabelCache(
    private val file: File,
) {
    private val stored: Map<String, String> by lazy { read() }
    private val used = HashMap<String, String>()
    private var dirty = false

    /** El nombre guardado para [key] o, si no lo hay, el que da [load] (y queda para el próximo arranque). */
    @Synchronized
    fun label(
        key: String,
        load: () -> String,
    ): String {
        val label = used[key] ?: stored[key] ?: load().also { dirty = true }
        used[key] = label
        return label
    }

    /**
     * Escribe la caché si ha entrado algún nombre nuevo. De cada actividad leída se queda sólo la clave
     * actual (la de un APK o un idioma anterior se descarta); las que no se leyeron esta vez (otro
     * perfil, otro paquete) se conservan.
     */
    @Synchronized
    fun save() {
        if (!dirty) return
        val current = used.keys.mapTo(HashSet()) { it.substringBefore('|') }
        val entries = stored.filterKeys { it.substringBefore('|') !in current } + used
        file.parentFile?.mkdirs()
        val temporary = File(file.path + ".tmp")
        temporary.writeText(entries.entries.joinToString("\n") { (key, label) -> "${escape(key)}\t${escape(label)}" })
        if (!temporary.renameTo(file)) temporary.delete()
        dirty = false
    }

    private fun read(): Map<String, String> =
        runCatching {
            if (!file.isFile) return@runCatching emptyMap()
            file
                .readLines()
                .mapNotNull { line ->
                    val tab = line.indexOf('\t')
                    if (tab <= 0) null else unescape(line.substring(0, tab)) to unescape(line.substring(tab + 1))
                }.toMap()
        }.getOrDefault(emptyMap())

    companion object {
        /** Clave de una actividad: cambia con el APK instalado y con el idioma. */
        fun key(
            component: String,
            userSerial: Long,
            apkPath: String,
            locale: String,
        ): String = "$component@$userSerial|$apkPath|$locale"

        private fun escape(text: String) = text.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n")

        private fun unescape(text: String): String {
            val out = StringBuilder(text.length)
            var i = 0
            while (i < text.length) {
                val c = text[i]
                if (c == '\\' && i + 1 < text.length) {
                    out.append(
                        when (text[i + 1]) {
                            't' -> '\t'
                            'n' -> '\n'
                            else -> text[i + 1]
                        },
                    )
                    i += 2
                } else {
                    out.append(c)
                    i++
                }
            }
            return out.toString()
        }
    }
}
