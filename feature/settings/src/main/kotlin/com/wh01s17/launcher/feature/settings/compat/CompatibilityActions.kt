package com.wh01s17.launcher.feature.settings.compat

/** Lo que la pantalla pide al sistema; la actividad lo implementa con [com.wh01s17.launcher.core.system.oem.xiaomi.SystemScreens]. */
class CompatibilityActions(
    val openAccessibility: () -> Unit,
    val openAppDetails: () -> Unit,
    val openAutostart: () -> Unit,
    val openDeveloperOptions: () -> Unit,
)
