package com.wh01s17.launcher.feature.settings.style

/** Android document pickers and navigation belong to the application. */
data class StyleActions(
    val wallpapers: () -> Unit,
    val importImage: () -> Unit,
    val systemWallpaper: () -> Unit,
    val importConfig: () -> Unit,
    val exportConfig: () -> Unit,
    val preview: () -> Unit,
    val dreamSettings: () -> Unit,
)
