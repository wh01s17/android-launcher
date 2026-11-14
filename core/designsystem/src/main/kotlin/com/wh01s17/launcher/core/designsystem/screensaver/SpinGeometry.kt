package com.wh01s17.launcher.core.designsystem.screensaver

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * La W de puntos de `fastfetch/w-spin.py` (dotfiles): la silueta de las cinco barras extruida,
 * muestreada una vez en superficie (dos caras y el canto), proyectada en perspectiva y sombreada
 * punto a punto. El resultado es la rejilla braille del terminal, 40 × 20 celdas de 2 × 4 puntos:
 * [DOT_WIDTH] × [DOT_HEIGHT] puntos con paso cuadrado. Sin la A de Arch: sólo la W.
 */
@Suppress("MagicNumber") // Geometría, luz y cámara son datos de w-spin.py.
object SpinGeometry {
    const val DOT_WIDTH = 80
    const val DOT_HEIGHT = 80

    /** Niveles de brillo: como `w-spin.py`, se cuantiza a dieciseisavos. */
    const val LEVELS = 16

    private val boxes2d =
        listOf(
            intArrayOf(88, 104, 168, 344),
            intArrayOf(216, 104, 296, 344),
            intArrayOf(344, 104, 424, 344),
            intArrayOf(152, 344, 232, 408),
            intArrayOf(280, 344, 360, 408),
        )

    /** Puntos de la superficie: x, y, z y la normal (nx, ny, nz), seis floats por punto. */
    private val surface: DoubleArray by lazy(::sampleSurface)

    private fun silhouette(
        x: Double,
        y: Double,
    ): Boolean {
        val sx = x * 168 + 256
        val sy = 256 - y * 168
        return boxes2d.any { box -> sx >= box[0] && sx <= box[2] && sy >= box[1] && sy <= box[3] }
    }

    private fun sampleSurface(): DoubleArray {
        val points = ArrayList<Double>()

        fun add(vararg values: Double) = values.forEach(points::add)
        val step = 1.0 / 38
        for (iy in -38..38) {
            for (ix in -38..38) {
                val x = ix * step
                val y = iy * step
                if (!silhouette(x, y)) continue
                add(x, y, -0.065, 0.0, 0.0, -1.0)
                add(x, y, 0.065, 0.0, 0.0, 1.0)
                for ((dx, dy) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                    if (silhouette(x + dx * step, y + dy * step)) continue
                    for (iz in -2..2) add(x, y, iz * 0.0325, dx.toDouble(), dy.toDouble(), 0.0)
                }
            }
        }
        return points.toDoubleArray()
    }

    /**
     * Brillo de cada punto con la W girada [yaw] radianes, fila a fila: 0 = apagado, si no, un
     * nivel entre 1 y [LEVELS] (de 25 % a 100 % del acento). Se queda el punto más cercano.
     */
    fun render(yaw: Double): IntArray {
        val cy = cos(yaw)
        val sy = sin(yaw)
        val cp = cos(-0.08)
        val sp = sin(-0.08)
        val depth = DoubleArray(DOT_WIDTH * DOT_HEIGHT) { Double.POSITIVE_INFINITY }
        val shine = DoubleArray(DOT_WIDTH * DOT_HEIGHT)
        val s = surface
        var i = 0
        while (i < s.size) {
            val x = s[i]
            val y = s[i + 1]
            val z = s[i + 2]
            val vx = cy * x + sy * z
            val vz0 = -sy * x + cy * z
            val vy = cp * y - sp * vz0
            val vz = sp * y + cp * vz0
            val k = 3.8 / (3.8 + vz)
            val px = (DOT_WIDTH / 2 + vx * 33 * k).roundToInt()
            val py = (DOT_HEIGHT / 2 - vy * 33 * k).roundToInt()
            val index = py * DOT_WIDTH + px
            if (px in 0 until DOT_WIDTH && py in 0 until DOT_HEIGHT && vz < depth[index]) {
                val nx = s[i + 3]
                val ny = s[i + 4]
                val nz = s[i + 5]
                val nxx = cy * nx + sy * nz
                val nzz0 = -sy * nx + cy * nz
                val nyy = cp * ny - sp * nzz0
                val nzz = sp * ny + cp * nzz0
                val light = max(0.0, -0.35 * nxx + 0.45 * nyy - 0.82 * nzz)
                // Profundidad y un reflejo que se mueve sombrean los puntos sin cambiar el tono.
                val lit = (0.5 + 0.42 * light + 0.08 * cos(y * 2 + yaw)) * (1 - 0.12 * max(0.0, vz))
                depth[index] = vz
                shine[index] = min(1.0, max(0.25, lit))
            }
            i += 6
        }
        return IntArray(DOT_WIDTH * DOT_HEIGHT) { if (depth[it].isInfinite()) 0 else max(1, (shine[it] * LEVELS).roundToInt()) }
    }

    /** Duración de un ciclo, como `WH01S17_SPIN_SECONDS` por defecto. */
    const val CYCLE_MILLIS = 7_200L

    /**
     * El giro de `w-spin.py --loop` con la W sola: quieta, dos vueltas con *ease-out*, pausa, otras
     * dos vueltas y quieta. [progress] va de 0 a 1 dentro del ciclo.
     */
    fun yaw(progress: Double): Double {
        fun turns(start: Double) = 4 * PI * easeOut((progress - start) / LEG)
        return when {
            progress < 0.06 -> 0.0
            progress < 0.06 + LEG -> turns(0.06)
            progress < 0.56 -> 0.0
            progress < 0.56 + LEG -> turns(0.56)
            else -> 0.0
        }
    }

    private const val LEG = 0.40

    private fun easeOut(t: Double) = t * t * t * (10 + t * (-15 + 6 * t))
}
