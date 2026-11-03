package com.wh01s17.launcher.core.fuzzy

import java.text.Normalizer

/**
 * Texto preparado para buscar: en minúsculas y sin tildes, con **la misma longitud** que el
 * original, de modo que una posición encontrada sirve para resaltar el texto original.
 *
 * [bonus] guarda, por posición, lo que vale empezar una coincidencia ahí (inicio de texto, de
 * palabra o de tramo *camelCase*). Se calcula una vez por candidato, no en cada búsqueda.
 */
class FoldedText private constructor(
    val original: String,
    internal val chars: CharArray,
    internal val bonus: IntArray,
) {
    val length: Int get() = chars.size

    /** [chars] como `String`, para comparar la consulta entera sin crear objetos en cada búsqueda. */
    internal val folded: String = String(chars)

    companion object {
        fun of(text: String): FoldedText {
            val chars = CharArray(text.length) { fold(text[it]) }
            val bonus = IntArray(text.length) { boundaryBonus(text, it) }
            return FoldedText(text, chars, bonus)
        }

        /** Un carácter, sin tilde y en minúsculas; si no se descompone, igual que estaba. */
        internal fun fold(c: Char): Char {
            if (c.code < ASCII_LIMIT) return c.lowercaseChar()
            val base = Normalizer.normalize(c.toString(), Normalizer.Form.NFD).firstOrNull() ?: c
            return base.lowercaseChar()
        }

        private fun boundaryBonus(
            text: String,
            index: Int,
        ): Int {
            val current = text[index]
            val previous = if (index == 0) null else text[index - 1]
            return when {
                !current.isLetterOrDigit() -> 0
                previous == null -> Scoring.BONUS_FIRST
                !previous.isLetterOrDigit() -> Scoring.BONUS_BOUNDARY
                previous.isLowerCase() && current.isUpperCase() -> Scoring.BONUS_CAMEL
                previous.isLetter() && current.isDigit() -> Scoring.BONUS_CAMEL
                else -> 0
            }
        }

        private const val ASCII_LIMIT = 0x80
    }
}

/** Pesos de la puntuación, a la manera de fzf. Sólo importan las proporciones entre ellos. */
internal object Scoring {
    const val MATCH = 16
    const val BONUS_FIRST = 10
    const val BONUS_BOUNDARY = 8
    const val BONUS_CAMEL = 7
    const val CONSECUTIVE = 5

    /** El primer carácter de la consulta pesa doble: es lo que más distingue lo que se busca. */
    const val FIRST_CHAR_MULTIPLIER = 2
    const val GAP_START = 3
    const val GAP_EXTENSION = 1

    /** El texto empieza por el término entero. */
    const val PREFIX = 24

    /** El texto es exactamente la consulta. */
    const val EXACT = 48

    /** Coincidir sólo por una palabra clave (p. ej. el paquete) vale menos que por el nombre. */
    const val KEYWORD_PENALTY = 16
}
