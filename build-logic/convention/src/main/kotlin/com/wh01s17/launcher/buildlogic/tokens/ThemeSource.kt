package com.wh01s17.launcher.buildlogic.tokens

import org.tomlj.Toml
import org.tomlj.TomlTable
import java.io.File

/** Error de contenido del tema: el generador falla en voz alta en vez de adivinar. */
class TokenException(
    message: String,
) : RuntimeException(message)

/**
 * Archivos del tema `wh01s17` ya parseados (plan.md §4.1).
 *
 * @property colors raíz de `colors.toml`.
 * @property sections tablas de cada `shell.<nombre>.toml`, por nombre de tabla (`bar`, `image-picker`…).
 * @property rounding radio de esquina de `hyprland.lua`.
 */
class ThemeSource(
    val colors: TomlTable,
    val sections: Map<String, TomlTable>,
    val rounding: Int,
) {
    companion object {
        private val ROUNDING = Regex("""(?m)^\s*rounding\s*=\s*(\d+)\s*,?\s*$""")

        fun load(dir: File): ThemeSource {
            val colors = parse(File(dir, "colors.toml"))
            val sections = linkedMapOf<String, TomlTable>()
            val shellFiles = dir.listFiles { f -> f.name.startsWith("shell.") && f.name.endsWith(".toml") }.orEmpty()
            for (file in shellFiles.sortedBy { it.name }) {
                val root = parse(file)
                val tables = root.keySet().filter { root.isTable(it) }
                if (tables.size != 1 || root.keySet().size != 1) {
                    throw TokenException("${file.name}: se espera exactamente una tabla de primer nivel")
                }
                sections[tables.single()] = root.getTable(tables.single())!!
            }
            if (sections.isEmpty()) throw TokenException("${dir.path}: no hay archivos shell.*.toml")
            return ThemeSource(colors, sections, parseRounding(File(dir, "hyprland.lua")))
        }

        private fun parse(file: File): TomlTable {
            if (!file.isFile) throw TokenException("falta ${file.path}")
            val result = Toml.parse(file.readText())
            if (result.hasErrors()) {
                throw TokenException("${file.name}: ${result.errors().joinToString("; ") { it.toString() }}")
            }
            return result
        }

        private fun parseRounding(file: File): Int {
            if (!file.isFile) throw TokenException("falta ${file.path}")
            val matches = ROUNDING.findAll(file.readText()).toList()
            if (matches.size != 1) {
                throw TokenException("${file.name}: se espera una sola línea `rounding = N` (hay ${matches.size})")
            }
            return matches.single().groupValues[1].toInt()
        }
    }
}
