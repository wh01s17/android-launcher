package com.wh01s17.launcher.core.system.timer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.IOException
import java.util.Properties

/** El metrónomo en un instante: sus ajustes, si suena y el tiempo actual (para los puntos). */
data class MetronomeState(
    val settings: MetronomeSettings = MetronomeSettings(),
    val running: Boolean = false,
    val beat: Int = 0,
    /**
     * Tiempos sonados desde que arrancó (`beatCount` del escritorio): el péndulo de la barra cambia
     * de lado con su paridad. [beat] no sirve: vuelve a 0 en cada compás y con un tiempo no cambia.
     */
    val ticks: Long = 0,
)

/**
 * El metrónomo del launcher (plan.md §7 Hito 7), compartido por el tile, la barra y el servicio en
 * primer plano. Los ajustes se guardan en [file]; si suena o no, no (al reabrir, en silencio).
 */
class MetronomeRepository(
    private val file: File,
    private val player: MetronomePlayer = MetronomePlayer(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutableState = MutableStateFlow(MetronomeState(load()))
    val state: StateFlow<MetronomeState> = mutableState.asStateFlow()

    private val taps = ArrayDeque<Long>()

    init {
        player.settings = mutableState.value.settings
    }

    fun toggle() = if (mutableState.value.running) stop() else start()

    fun start() {
        mutableState.update { it.copy(running = true, beat = 0, ticks = 0) }
        player.start { beat -> mutableState.update { it.copy(beat = beat, ticks = it.ticks + 1) } }
    }

    fun stop() {
        player.stop()
        mutableState.update { it.copy(running = false, beat = 0, ticks = 0) }
    }

    /** Cambia los ajustes (tempo, compás, subdivisión, acento); se ajustan a sus límites y se guardan. */
    fun edit(transform: (MetronomeSettings) -> MetronomeSettings) {
        val next = transform(mutableState.value.settings).normalized()
        if (next == mutableState.value.settings) return
        player.settings = next
        mutableState.update { it.copy(settings = next) }
        save(next)
    }

    /** Un toque de tap tempo; con dos o más seguidos fija el tempo. */
    fun tap() {
        val now = clock()
        if (taps.isNotEmpty() && now - taps.last() > TAP_RESET_MILLIS) taps.clear()
        taps.addLast(now)
        while (taps.size > MAX_TAPS) taps.removeFirst()
        MetronomeMath.tapTempo(taps.toList())?.let { bpm -> edit { it.copy(bpm = bpm) } }
    }

    private fun load(): MetronomeSettings {
        val p =
            try {
                file.takeIf { it.isFile }?.inputStream()?.use { Properties().apply { load(it) } }
            } catch (ignored: IOException) {
                null
            } catch (ignored: IllegalArgumentException) {
                null
            } ?: return MetronomeSettings()
        val defaults = MetronomeSettings()
        return MetronomeSettings(
            bpm = p.getProperty(BPM)?.toIntOrNull() ?: defaults.bpm,
            beats = p.getProperty(BEATS)?.toIntOrNull() ?: defaults.beats,
            subdivision = Subdivision.fromConfig(p.getProperty(SUBDIVISION).orEmpty()) ?: defaults.subdivision,
            accent = p.getProperty(ACCENT)?.toBooleanStrictOrNull() ?: defaults.accent,
            volume = p.getProperty(VOLUME)?.toFloatOrNull() ?: defaults.volume,
        ).normalized()
    }

    private fun save(settings: MetronomeSettings) {
        val p =
            Properties().apply {
                setProperty(BPM, settings.bpm.toString())
                setProperty(BEATS, settings.beats.toString())
                setProperty(SUBDIVISION, settings.subdivision.configName)
                setProperty(ACCENT, settings.accent.toString())
                setProperty(VOLUME, settings.volume.toString())
            }
        try {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.outputStream().use { p.store(it, null) }
            temp.renameTo(file)
        } catch (ignored: IOException) {
            // Sin disco los ajustes valen hasta cerrar la app.
        }
    }

    private companion object {
        const val BPM = "bpm"
        const val BEATS = "beats"
        const val SUBDIVISION = "subdivision"
        const val ACCENT = "accent"
        const val VOLUME = "volume"
        const val MAX_TAPS = 8
        const val TAP_RESET_MILLIS = 2000L
    }
}
