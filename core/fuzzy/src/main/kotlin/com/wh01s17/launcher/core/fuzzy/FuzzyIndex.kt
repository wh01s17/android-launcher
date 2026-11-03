package com.wh01s17.launcher.core.fuzzy

import kotlin.math.ln
import kotlin.math.pow

/** Cuánto y cuándo se usó algo, para subirlo en la lista (plan.md §5.6). */
data class Usage(
    val launchCount: Int,
    val lastUsedAtMillis: Long,
)

/** Un resultado. [positions] son las del texto principal que coinciden (vacío si fue por palabra clave). */
class FuzzyResult<T>(
    val item: T,
    val score: Int,
    val positions: IntArray,
)

/**
 * Búsqueda difusa sobre una lista fija (plan.md §5.6): cada palabra de la consulta debe ser
 * subsecuencia del texto (o estar seguida en una palabra clave, p. ej. el paquete) y la puntuación premia
 * inicios de palabra, letras seguidas y prefijos; el uso suma un poco para desempatar.
 *
 * Los textos se pliegan al construir el índice: buscar sólo ejecuta la DP. Es inmutable y cada
 * búsqueda usa su propio [FuzzyMatcher], así que se puede llamar desde varios hilos.
 */
class FuzzyIndex<T>(
    items: List<T>,
    text: (T) -> String,
    keywords: (T) -> List<String> = { emptyList() },
) {
    private class Entry<T>(
        val item: T,
        val order: Int,
        val text: FoldedText,
        val keywords: List<FoldedText>,
    )

    private val entries = items.mapIndexed { i, item -> Entry(item, i, FoldedText.of(text(item)), keywords(item).map(FoldedText::of)) }

    val size: Int get() = entries.size

    /**
     * Resultados de mejor a peor; a igual puntuación, en el orden de la lista original. Con la
     * consulta vacía devuelve todo, ordenado sólo por uso.
     */
    fun search(
        query: String,
        usage: (T) -> Usage? = { null },
        nowMillis: Long = 0,
        limit: Int = Int.MAX_VALUE,
    ): List<FuzzyResult<T>> {
        val terms =
            query.trim().split(WHITESPACE).filter { it.isNotEmpty() }.map { term ->
                CharArray(term.length) { FoldedText.fold(term[it]) }
            }
        val whole = terms.joinToString(" ") { String(it) }
        val matcher = FuzzyMatcher()
        val results = ArrayList<Pair<Entry<T>, FuzzyResult<T>>>()
        for (entry in entries) {
            val matched = if (terms.isEmpty()) EMPTY_MATCH else matchAll(matcher, terms, whole, entry) ?: continue
            val score = matched.score + usageBonus(usage(entry.item), nowMillis)
            results += entry to FuzzyResult(entry.item, score, matched.positions)
        }
        return results
            .sortedWith(compareByDescending<Pair<Entry<T>, FuzzyResult<T>>> { it.second.score }.thenBy { it.first.order })
            .take(limit.coerceAtLeast(0))
            .map { it.second }
    }

    private fun matchAll(
        matcher: FuzzyMatcher,
        terms: List<CharArray>,
        whole: String,
        entry: Entry<T>,
    ): TermMatch? {
        var score = if (entry.text.folded == whole) Scoring.EXACT else 0
        val positions = sortedSetOf<Int>()
        for (term in terms) {
            val direct = matcher.match(term, entry.text)?.let { TermMatch(it.score + prefixBonus(term, entry.text), it.positions) }
            // En las palabras clave el término va seguido: en `com.google.android.apps.x` cabe casi
            // cualquier subsecuencia, y eso llenaría la lista de ruido.
            val viaKeyword =
                entry.keywords
                    .filter {
                        it.folded.contains(
                            String(term),
                        )
                    }.mapNotNull { matcher.match(term, it) }
                    .maxByOrNull { it.score }
            when {
                direct != null && (viaKeyword == null || direct.score >= viaKeyword.score - Scoring.KEYWORD_PENALTY) -> {
                    score += direct.score
                    direct.positions.forEach { positions += it }
                }
                viaKeyword != null -> score += viaKeyword.score - Scoring.KEYWORD_PENALTY
                else -> return null
            }
        }
        return TermMatch(score, positions.toIntArray())
    }

    private fun prefixBonus(
        term: CharArray,
        text: FoldedText,
    ): Int = if (term.size <= text.length && term.indices.all { text.chars[it] == term[it] }) Scoring.PREFIX else 0

    companion object {
        private val WHITESPACE = Regex("\\s+")
        private val EMPTY_MATCH = TermMatch(0, IntArray(0))

        private const val FREQUENCY_WEIGHT = 4.0
        private const val FREQUENCY_CAP = 24
        private const val RECENCY_MAX = 16.0
        private const val RECENCY_HALF_LIFE_MILLIS = 3 * 24 * 60 * 60 * 1000.0

        /**
         * Hasta 24 puntos por frecuencia (logarítmica: la app número 100 no aplasta a todas) y hasta
         * 16 por recencia, que se reducen a la mitad cada tres días. Una letra de más en la
         * consulta vale 16: el uso desempata, no ignora lo que se escribe.
         */
        internal fun usageBonus(
            usage: Usage?,
            nowMillis: Long,
        ): Int {
            if (usage == null || usage.launchCount <= 0) return 0
            val frequency = (ln(1.0 + usage.launchCount) / ln(2.0) * FREQUENCY_WEIGHT).toInt().coerceAtMost(FREQUENCY_CAP)
            val age = (nowMillis - usage.lastUsedAtMillis).coerceAtLeast(0)
            val recency = (RECENCY_MAX * 0.5.pow(age / RECENCY_HALF_LIFE_MILLIS)).toInt()
            return frequency + recency
        }
    }
}
