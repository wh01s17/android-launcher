package com.wh01s17.launcher.core.system.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WallpaperStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `a broken replacement preserves the imported image`() {
        val store = WallpaperStore(context)
        val source = File(context.cacheDir, "test.png")
        val bitmap = Bitmap.createBitmap(16, 32, Bitmap.Config.ARGB_8888)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        store.importImage(Uri.fromFile(source))
        assertEquals(16, store.load("imported")!!.width)
        val version = store.revision.value
        source.writeText("invalid image")
        try {
            store.importImage(Uri.fromFile(source))
            throw AssertionError("invalid image was accepted")
        } catch (_: IOException) {
            assertEquals(version, store.revision.value)
            assertEquals(32, store.load("imported")!!.height)
        }
    }

    @Test
    fun `large images are sampled before being stored`() {
        val store = WallpaperStore(context)
        val source = File(context.cacheDir, "large.png")
        val bitmap = Bitmap.createBitmap(8193, 16, Bitmap.Config.ARGB_8888)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        store.importImage(Uri.fromFile(source))
        assertTrue(store.load("imported")!!.width <= 2048)
    }
}
