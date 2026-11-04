package com.wh01s17.launcher.feature.bar.module

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.designsystem.components.WhIcon
import com.wh01s17.launcher.core.designsystem.icons.WhGlyph
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.system.status.AudioState
import com.wh01s17.launcher.feature.bar.R

/** `wh01s17.audio`: volumen de medios. Clases: `muted`, `low` (< 50 %) y `high`. Silenciado se atenúa. */
internal class AudioModule : BarModule {
    override val id = ID

    @Composable
    override fun Content(context: BarModuleContext) {
        val audio = context.state.audio
        val style = rememberModuleStyle(context.settings, listOf(classOf(audio)), moduleDefaults(dimClasses = setOf(MUTED)))
        val description =
            if (audio.muted) stringResource(R.string.bar_audio_muted) else stringResource(R.string.bar_audio_level, audio.volumePercent)
        BarItem(style, contentDescription = description) { WhIcon(glyphOf(audio)) }
    }

    companion object {
        const val ID = "wh01s17.audio"
        const val MUTED = "muted"
        const val LOW = "low"
        const val HIGH = "high"

        fun classOf(audio: AudioState): String =
            when {
                audio.muted -> MUTED
                audio.volumePercent < AudioState.LOW_PERCENT -> LOW
                else -> HIGH
            }

        fun glyphOf(audio: AudioState): WhGlyph =
            when (classOf(audio)) {
                MUTED -> WhIcons.VolumeMute
                LOW -> WhIcons.VolumeLow
                else -> WhIcons.VolumeHigh
            }
    }
}
