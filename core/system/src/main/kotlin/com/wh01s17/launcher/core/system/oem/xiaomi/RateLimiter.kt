package com.wh01s17.launcher.core.system.oem.xiaomi

/** Como mucho [max] permisos en cualquier ventana de [windowMillis]; para no pelear con el sistema. */
class RateLimiter(
    private val max: Int,
    private val windowMillis: Long,
    private val clock: () -> Long,
) {
    private val taken = ArrayDeque<Long>()

    /** `true` y lo cuenta si aún queda cupo en la ventana actual. */
    fun tryAcquire(): Boolean {
        val now = clock()
        while (taken.isNotEmpty() && now - taken.first() >= windowMillis) taken.removeFirst()
        if (taken.size >= max) return false
        taken.addLast(now)
        return true
    }

    fun reset() = taken.clear()
}
