package com.wh01s17.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wh01s17.launcher.core.system.wallpaper.WallpaperStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
internal fun HomeWallpaper(
    id: String?,
    store: WallpaperStore,
) {
    val revision by store.revision.collectAsStateWithLifecycle()
    val image by produceState<ImageBitmap?>(null, id, revision) {
        value =
            withContext(Dispatchers.IO) {
                try {
                    store.load(id ?: "none")?.asImageBitmap()
                } catch (_: IOException) {
                    null
                }
            }
    }
    image?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
}
