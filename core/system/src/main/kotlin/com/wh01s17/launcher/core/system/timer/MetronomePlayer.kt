package com.wh01s17.launcher.core.system.timer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process

/**
 * Reproduce el metrónomo con un `AudioTrack` en modo stream: cada tiempo se sintetiza entero
 * ([MetronomeMath.renderBeat]) y se escribe a continuación del anterior, así que el ritmo lo marca
 * el reloj del audio, no un temporizador. Los cambios de ajustes entran en el tiempo siguiente,
 * como en `metronome-engine.py`. [onBeat] se llama (en el hilo del audio) al encolar cada tiempo.
 */
class MetronomePlayer(
    private val rate: Int = SAMPLE_RATE,
) {
    @Volatile
    var settings: MetronomeSettings = MetronomeSettings()

    @Volatile
    private var thread: Thread? = null

    val playing: Boolean get() = thread != null

    fun start(onBeat: (Int) -> Unit) {
        if (thread != null) return
        thread = Thread({ loop(onBeat) }, "wh01s17-metronome").also { it.start() }
    }

    fun stop() {
        val current = thread ?: return
        thread = null
        current.interrupt()
    }

    private fun loop(onBeat: (Int) -> Unit) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val track = createTrack()
        track.play()
        var beat = 0
        try {
            while (thread === Thread.currentThread()) {
                val current = settings
                beat %= current.beats
                val pcm = MetronomeMath.renderBeat(current, beat, rate)
                onBeat(beat)
                if (track.write(pcm, 0, pcm.size) < 0) break
                beat++
            }
        } finally {
            track.pause()
            track.flush()
            track.release()
        }
    }

    private fun createTrack(): AudioTrack {
        val minBuffer = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        return AudioTrack
            .Builder()
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            ).setAudioFormat(
                AudioFormat
                    .Builder()
                    .setSampleRate(rate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            ).setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(minBuffer * 2)
            .build()
    }

    private companion object {
        const val SAMPLE_RATE = 48_000
    }
}
