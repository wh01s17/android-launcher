package com.wh01s17.launcher.core.system.navigation

import com.wh01s17.launcher.core.system.oem.xiaomi.RateLimiter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnabledServicesTest {
    private val pkg = "com.wh01s17.launcher.debug"
    private val cls = "com.wh01s17.launcher.core.system.navigation.WhNavigationService"
    private val ours = "$pkg/$cls"
    private val talkback = "com.google.android.marvin.talkback/.TalkBackService"

    @Test
    fun `removing ours keeps the other services`() {
        assertEquals(talkback, EnabledServices.without("$talkback:$ours", pkg, cls))
        assertEquals("", EnabledServices.without(ours, pkg, cls))
        // Forma corta (`paquete/.Clase`): sólo vale cuando la clase cuelga del paquete, como en release.
        val release = "com.wh01s17.launcher"
        assertEquals(talkback, EnabledServices.without("$release/.core.system.navigation.WhNavigationService:$talkback", release, cls))
        // En debug el paquete lleva `.debug`: `paquete/.Clase` sería otra clase.
        assertNull(EnabledServices.without("$pkg/.core.system.navigation.WhNavigationService", pkg, cls))
    }

    @Test
    fun `a service the user disabled is not revived`() {
        assertNull(EnabledServices.without(talkback, pkg, cls))
        assertNull(EnabledServices.without(null, pkg, cls))
        assertNull(EnabledServices.without("\"\"", pkg, cls))
        // Otra variante del mismo paquete no cuenta.
        assertNull(EnabledServices.without("com.wh01s17.launcher/$cls", pkg, cls))
    }

    @Test
    fun `adding back is idempotent and tolerates junk`() {
        assertEquals("$talkback:$ours", EnabledServices.with(talkback, ours))
        assertEquals("$talkback:$ours", EnabledServices.with("$talkback:$ours", ours))
        assertEquals(ours, EnabledServices.with("\"\"", ours))
        assertEquals(ours, EnabledServices.with(null, ours))
    }

    @Test
    fun `rate limiter counts a sliding window`() {
        var now = 0L
        val limiter = RateLimiter(max = 2, windowMillis = 100, clock = { now })
        assertTrue(limiter.tryAcquire())
        assertTrue(limiter.tryAcquire())
        assertFalse(limiter.tryAcquire())
        now = 100
        assertTrue(limiter.tryAcquire())
        limiter.reset()
        assertTrue(limiter.tryAcquire())
    }
}
