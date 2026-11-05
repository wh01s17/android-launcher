package com.wh01s17.launcher.feature.settings.style

/** Los selectores de documentos de Android y la navegación son de la aplicación. */
data class StyleActions(
    val wallpapers: () -> Unit,
    val importImage: () -> Unit,
    val systemWallpaper: () -> Unit,
    val importConfig: () -> Unit,
    val exportConfig: () -> Unit,
    val preview: () -> Unit,
    val dreamSettings: () -> Unit,
)
