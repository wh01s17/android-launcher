package com.wh01s17.launcher.core.system.status

import android.content.Context
import android.media.AudioManager
import kotlinx.coroutines.flow.Flow

/** Volumen de medios (módulo `wh01s17.audio`). */
data class AudioState(
    /** 0–100. */
    val volumePercent: Int,
    val muted: Boolean,
) {
    companion object {
        private const val FULL = 100
        const val LOW_PERCENT = 50

        fun of(
            volume: Int,
            max: Int,
            streamMuted: Boolean,
        ): AudioState {
            val percent = if (max > 0) (volume * FULL / max).coerceIn(0, FULL) else 0
            return AudioState(percent, muted = streamMuted || volume == 0)
        }
    }
}

/**
 * [AudioState] del flujo de medios. Android no ofrece un callback público del volumen: se escuchan
 * las difusiones del sistema, que usan casi todas las apps de volumen.
 */
class AudioSource(
    private val context: Context,
) : StatusSource<AudioState> {
    private val audio = context.getSystemService(AudioManager::class.java)

    override fun observe(): Flow<AudioState> =
        context.broadcastState(
            intentFilterOf(VOLUME_CHANGED, STREAM_MUTE_CHANGED, MASTER_MUTE_CHANGED, AudioManager.RINGER_MODE_CHANGED_ACTION),
        ) {
            AudioState.of(
                volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC),
                max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                streamMuted = audio.isStreamMute(AudioManager.STREAM_MUSIC),
            )
        }

    private companion object {
        // Acciones del sistema sin constante pública en el SDK.
        const val VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
        const val STREAM_MUTE_CHANGED = "android.media.STREAM_MUTE_CHANGED_ACTION"
        const val MASTER_MUTE_CHANGED = "android.media.MASTER_MUTE_CHANGED_ACTION"
    }
}
