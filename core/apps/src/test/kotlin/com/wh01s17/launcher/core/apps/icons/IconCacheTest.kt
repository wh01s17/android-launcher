package com.wh01s17.launcher.core.apps.icons

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.wh01s17.launcher.core.apps.model.AppKey
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class IconCacheTest {
    private fun request(
        pkg: String,
        stamp: Long = 0,
    ) = IconRequest(AppKey(pkg, "$pkg.Main", 0), stamp, IconStyle.Monochrome, 16, Color.WHITE)

    private fun bitmap(): Bitmap = createBitmap(16, 16) // 16 × 16 × 4 = 1024 bytes

    @Test
    fun `keeps what fits and evicts the least recently used`() {
        val cache = IconCache(maxBytes = 2048)
        val a = bitmap()
        cache.put(request("a"), a)
        cache.put(request("b"), bitmap())
        assertSame(a, cache[request("a")]) // "a" pasa a ser el más reciente
        cache.put(request("c"), bitmap())
        assertNull(cache[request("b")])
        assertNotNull(cache[request("a")])
        assertNotNull(cache[request("c")])
        assertTrue(cache.sizeBytes <= 2048)
    }

    @Test
    fun `a new stamp is a different icon`() {
        val cache = IconCache(maxBytes = 4096)
        cache.put(request("a", stamp = 1), bitmap())
        assertNull(cache[request("a", stamp = 2)])
    }
}
