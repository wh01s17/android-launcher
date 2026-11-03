package com.wh01s17.launcher.core.fuzzy

/** Una coincidencia: su puntuación y las posiciones del texto que la forman, en orden. */
class TermMatch(
    val score: Int,
    val positions: IntArray,
)

/**
 * Coincidencia por subsecuencia con la mejor alineación (programación dinámica, como fzf v2).
 *
 * `M[i][j]` es la mejor puntuación si el carácter `i` del término cae en la posición `j` del
 * texto. Se llega desde `M[i-1][j-1]` (seguidos, con bonus) o desde cualquier `M[i-1][k]` anterior
 * pagando el hueco; el mejor `k` se arrastra en una variable, así que el coste es `O(m·n)`.
 *
 * Reutiliza sus búferes entre llamadas: una instancia por hilo (o por búsqueda).
 */
class FuzzyMatcher {
    private var scores = IntArray(0)
    private var previous = IntArray(0)

    // Hueco arrastrado de la fila en curso: mejor M[i-1][k] menos su penalización, y su k.
    private var gapBest = NONE
    private var gapFrom = -1

    /** `null` si [term] (ya plegado, ver [FoldedText.fold]) no es subsecuencia de [text]. */
    fun match(
        term: CharArray,
        text: FoldedText,
    ): TermMatch? {
        val m = term.size
        val n = text.length
        if (m == 0 || m > n || !isSubsequence(term, text.chars)) return null
        ensureCapacity(m * n)
        for (i in 0 until m) fillRow(i, term[i], text)
        var bestJ = -1
        var best = NONE
        val last = (m - 1) * n
        for (j in m - 1 until n) {
            if (scores[last + j] > best) {
                best = scores[last + j]
                bestJ = j
            }
        }
        return TermMatch(best, backtrack(m, n, bestJ))
    }

    private fun fillRow(
        i: Int,
        c: Char,
        text: FoldedText,
    ) {
        val n = text.length
        val row = i * n
        gapBest = NONE
        gapFrom = -1
        for (j in 0 until n) {
            if (i > 0 && j >= 2) advanceGap(row - n + j - 2, j - 2)
            if (text.chars[j] == c) {
                scores[row + j] = cellScore(i, j, n, text.bonus[j])
            } else {
                scores[row + j] = NONE
                previous[row + j] = -1
            }
        }
    }

    /**
     * Mueve el hueco una posición: el mejor origen anterior paga una unidad más y entra como
     * candidato el que acaba de quedar a dos posiciones ([index] en la fila de arriba).
     */
    private fun advanceGap(
        index: Int,
        from: Int,
    ) {
        val opened = scores[index].let { if (it == NONE) NONE else it - Scoring.GAP_START }
        val extended = if (gapBest == NONE) NONE else gapBest - Scoring.GAP_EXTENSION
        if (opened >= extended) {
            gapBest = opened
            gapFrom = if (opened == NONE) -1 else from
        } else {
            gapBest = extended
        }
    }

    /** Puntuación de `M[i][j]` (el carácter coincide) y de dónde viene, en [previous]. */
    private fun cellScore(
        i: Int,
        j: Int,
        n: Int,
        bonus: Int,
    ): Int {
        val cell = i * n + j
        previous[cell] = -1
        if (i == 0) return Scoring.MATCH + bonus * Scoring.FIRST_CHAR_MULTIPLIER
        val diagonal = if (j >= 1) scores[cell - n - 1] else NONE
        val consecutive = if (diagonal == NONE) NONE else diagonal + Scoring.MATCH + maxOf(bonus, Scoring.CONSECUTIVE)
        val gapped = if (gapBest == NONE) NONE else gapBest + Scoring.MATCH + bonus
        return when {
            consecutive != NONE && consecutive >= gapped -> consecutive.also { previous[cell] = j - 1 }
            gapped != NONE -> gapped.also { previous[cell] = gapFrom }
            else -> NONE
        }
    }

    private fun backtrack(
        m: Int,
        n: Int,
        lastJ: Int,
    ): IntArray {
        val positions = IntArray(m)
        var j = lastJ
        for (i in m - 1 downTo 0) {
            positions[i] = j
            j = previous[i * n + j]
        }
        return positions
    }

    private fun ensureCapacity(size: Int) {
        if (scores.size < size) {
            scores = IntArray(size)
            previous = IntArray(size)
        }
    }

    private companion object {
        /** "Imposible": tan bajo que ninguna suma de bonus lo alcanza. */
        const val NONE = Int.MIN_VALUE / 2

        /** Descarte rápido antes de la DP: la mayoría de candidatos no contiene la consulta. */
        fun isSubsequence(
            term: CharArray,
            text: CharArray,
        ): Boolean {
            var i = 0
            for (c in text) {
                if (c == term[i] && ++i == term.size) return true
            }
            return false
        }
    }
}
