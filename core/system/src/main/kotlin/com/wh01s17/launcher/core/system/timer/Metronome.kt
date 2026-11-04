package com.wh01s17.launcher.core.system.timer

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/** Subdivisiones de `Model.js` del metrónomo de los dotfiles: dónde cae cada pulso dentro del tiempo. */
@Suppress("MagicNumber") // Datos de `Model.js`, copiados tal cual.
enum class Subdivision(
    val configName: String,
    val label: String,
    val offsets: List<Double>,
) {
    Quarter("1/4", "1/4", listOf(0.0)),
    Eighth("1/8", "1/8", listOf(0.0, 0.5)),
    EighthTriplet("1/8t", "1/8T", listOf(0.0, 1.0 / 3, 2.0 / 3)),
    Swing("swing", "SWING", listOf(0.0, 2.0 / 3)),
    Sixteenth("1/16", "1/16", listOf(0.0, 0.25, 0.5, 0.75)),
    SixteenthTriplet("1/16t", "1/16T", (0 until 6).map { it / 6.0 }),
    ;

    companion object {
        fun fromConfig(name: String): Subdivision? = entries.firstOrNull { it.configName == name }
    }
}

/** Voces de `metronome-engine.py`: frecuencia, amplitud, constante de caída y duración. */
@Suppress("MagicNumber") // Datos de `metronome-engine.py`, copiados tal cual.
enum class ClickVoice(
    val hz: Double,
    val amplitude: Double,
    val decaySeconds: Double,
    val lengthSeconds: Double,
) {
    Accent(1560.0, 1.0, 0.016, 0.055),
    Beat(1040.0, 0.72, 0.018, 0.055),
    Sub(2080.0, 0.30, 0.008, 0.030),
}

data class MetronomeSettings(
    val bpm: Int = DEFAULT_BPM,
    val beats: Int = DEFAULT_BEATS,
    val subdivision: Subdivision = Subdivision.Quarter,
    val accent: Boolean = true,
    val volume: Float = DEFAULT_VOLUME,
) {
    /** «100 BPM · 4/4 · 1/4», como `signature` del escritorio. */
    val signature: String get() = "$bpm BPM · $beats/4 · ${subdivision.label}"

    fun normalized(): MetronomeSettings =
        copy(bpm = bpm.coerceIn(BPM_RANGE), beats = beats.coerceIn(BEATS_RANGE), volume = volume.coerceIn(0f, 1f))

    companion object {
        const val DEFAULT_BPM = 100
        const val DEFAULT_BEATS = 4
        const val DEFAULT_VOLUME = 0.7f
        val BPM_RANGE = 40..240
        val BEATS_RANGE = 1..16
    }
}

/** Partes puras del metrónomo: qué suena en cada tiempo y cómo suena (se prueban en la JVM). */
object MetronomeMath {
    @Suppress("MagicNumber") // Marcas de tempo de `Model.js`.
    private val TEMPO_MARKS =
        listOf(
            24 to "Larghissimo",
            40 to "Grave",
            60 to "Largo",
            66 to "Larghetto",
            76 to "Adagio",
            108 to "Andante",
            120 to "Moderato",
            156 to "Allegro",
            176 to "Vivace",
            200 to "Presto",
        )

    fun tempoName(bpm: Int): String = TEMPO_MARKS.firstOrNull { bpm <= it.first }?.second ?: "Prestissimo"

    /** Muestras de un tiempo a [rate] Hz. */
    fun beatFrames(
        bpm: Int,
        rate: Int,
    ): Int = (rate * SECONDS_PER_MINUTE / bpm).roundToInt()

    /** Las voces de los pulsos del tiempo [beat] (0 = primer tiempo del compás), en orden. */
    fun voices(
        settings: MetronomeSettings,
        beat: Int,
    ): List<ClickVoice> =
        settings.subdivision.offsets.indices.map { pulse ->
            when {
                pulse > 0 -> ClickVoice.Sub
                beat == 0 && settings.accent -> ClickVoice.Accent
                else -> ClickVoice.Beat
            }
        }

    /**
     * El audio de un tiempo completo: los clics de sus pulsos (seno con caída exponencial, como el
     * motor del escritorio) y silencio hasta el siguiente. PCM de 16 bits mono.
     */
    fun renderBeat(
        settings: MetronomeSettings,
        beat: Int,
        rate: Int,
    ): ShortArray {
        val frames = beatFrames(settings.bpm, rate)
        val out = DoubleArray(frames)
        val voices = voices(settings, beat)
        settings.subdivision.offsets.forEachIndexed { pulse, offset ->
            val voice = voices[pulse]
            val start = (offset * frames).roundToInt()
            val length = (voice.lengthSeconds * rate).roundToInt()
            val omega = 2 * PI * voice.hz / rate
            for (i in 0 until minOf(length, frames - start)) {
                val envelope = exp(-(i.toDouble() / rate) / voice.decaySeconds)
                out[start + i] += voice.amplitude * settings.volume * envelope * sin(omega * i)
            }
        }
        return ShortArray(frames) { (out[it].coerceIn(-1.0, 1.0) * Short.MAX_VALUE).roundToInt().toShort() }
    }

    /** Tap tempo: media de los intervalos; un hueco de más de 2 s empieza una medida nueva. */
    fun tapTempo(tapsMillis: List<Long>): Int? {
        val recent = tapsMillis.zipWithNext().map { (a, b) -> b - a }.takeLastWhile { it <= TAP_RESET_MILLIS }
        if (recent.isEmpty()) return null
        return (SECONDS_PER_MINUTE * MILLIS_PER_SECOND / recent.average()).roundToInt().coerceIn(MetronomeSettings.BPM_RANGE)
    }

    private const val SECONDS_PER_MINUTE = 60.0
    private const val MILLIS_PER_SECOND = 1000.0
    private const val TAP_RESET_MILLIS = 2000L
}
