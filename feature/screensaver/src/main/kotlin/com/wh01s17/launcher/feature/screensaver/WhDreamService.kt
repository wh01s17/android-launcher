package com.wh01s17.launcher.feature.screensaver

import android.service.dreams.DreamService
import com.wh01s17.launcher.core.designsystem.palette.accentsOf
import com.wh01s17.launcher.core.designsystem.screensaver.WhSpinView

/** Android decide cuándo corre el salvapantallas (al cargar o en la base). Un toque o una tecla lo cierra. */
class WhDreamService : DreamService() {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false
        setContentView(WhSpinView(this, accents = accentsOf(application)))
    }
}
