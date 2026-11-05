package com.wh01s17.launcher.feature.widgets.native

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.timer.ClickVoice

/**
 * Un punto del compás: siempre del color del sonido que tendrá ese tiempo (acento en verde, tiempo
 * normal en cian, como los clics de `metronome-engine.py`); el que suena ahora, más grande.
 */
@Composable
internal fun BeatDot(
    voice: ClickVoice,
    sounding: Boolean,
    small: Boolean = false,
) {
    val size =
        WhTheme.spacing.controlGap *
            if (sounding) {
                SOUNDING_DOT
            } else if (small) {
                1
            } else {
                2
            }
    Box(Modifier.size(size).background(voiceColor(voice), CircleShape))
}

@Composable
internal fun voiceColor(voice: ClickVoice) =
    when (voice) {
        ClickVoice.Accent -> WhTheme.colors.accent
        ClickVoice.Beat -> WhTheme.colors.cyan
        ClickVoice.Sub -> WhTheme.colors.darkForeground
    }

private const val SOUNDING_DOT = 3
