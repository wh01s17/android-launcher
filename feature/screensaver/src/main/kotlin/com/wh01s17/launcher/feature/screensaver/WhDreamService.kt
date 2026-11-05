package com.wh01s17.launcher.feature.screensaver

import android.service.dreams.DreamService
import com.wh01s17.launcher.core.designsystem.screensaver.WhSpinView

/** Android controls when the charging/docked dream runs. Touch or a key dismisses it. */
class WhDreamService : DreamService() {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false
        setContentView(WhSpinView(this))
    }
}
