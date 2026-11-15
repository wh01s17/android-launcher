package com.wh01s17.launcher.feature.screensaver

import android.service.dreams.DreamService
import com.wh01s17.launcher.core.designsystem.palette.palettesOf
import com.wh01s17.launcher.core.designsystem.screensaver.WhScreensaverView
import com.wh01s17.launcher.core.designsystem.screensaver.screensaverSettingsOf

/** Android decide cuándo corre el salvapantallas (al cargar o en la base). Un toque o una tecla lo cierra. */
class WhDreamService : DreamService() {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false
        setContentView(WhScreensaverView(this, palettesOf(application), screensaverSettingsOf(application)))
    }
}
