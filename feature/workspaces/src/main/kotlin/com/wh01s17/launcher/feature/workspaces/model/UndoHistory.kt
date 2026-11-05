package com.wh01s17.launcher.feature.workspaces.model

/**
 * Los pasos para deshacer cambios de los escritorios («deshacer» del modo edición): cada cambio de
 * la distribución guarda cómo estaba todo antes. El foco y el escritorio actual no cuentan (tocar un
 * tile no es algo que deshacer), y los cambios seguidos con la misma [record] `key` en menos de
 * [COALESCE_MILLIS] (los pasos de un mismo arrastre de borde) son uno solo. Guarda [limit] pasos.
 */
internal class UndoHistory(
    private val limit: Int = DEFAULT_LIMIT,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val steps = ArrayDeque<Desk>()
    private var lastKey: String? = null
    private var lastAt = 0L

    val size: Int get() = steps.size

    fun record(
        before: Desk,
        after: Desk,
        key: String? = null,
    ) {
        if (layoutOf(before) == layoutOf(after)) return
        val now = clock()
        val sameGesture = key != null && key == lastKey && now - lastAt < COALESCE_MILLIS
        lastKey = key
        lastAt = now
        if (sameGesture) return
        steps.addLast(before)
        while (steps.size > limit) steps.removeFirst()
    }

    /** El estado anterior al último cambio, o `null` si no queda nada que deshacer. */
    fun pop(): Desk? {
        lastKey = null
        return steps.removeLastOrNull()
    }

    private fun layoutOf(desk: Desk) = desk.workspaces.map { it.copy(focused = null, history = emptyList()) } to desk.contents

    companion object {
        const val DEFAULT_LIMIT = 50
        const val COALESCE_MILLIS = 600L
    }
}
