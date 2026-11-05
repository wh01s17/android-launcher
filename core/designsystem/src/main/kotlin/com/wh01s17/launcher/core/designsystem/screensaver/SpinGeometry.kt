package com.wh01s17.launcher.core.designsystem.screensaver

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Ray-box renderer ported from dotfiles fastfetch/w-spin.py, on its 40 × 20 ASCII grid. */
@Suppress("MagicNumber") // Geometry, lighting and camera are data from w-spin.py.
object SpinGeometry {
    const val WIDTH = 40
    const val HEIGHT = 20
    private const val RAMP = " .,:;irsXA253hMHGS#9B&@"
    private val boxes =
        listOf(
            Box(V(-168.0, -88.0, -32.0), V(-88.0, 152.0, 32.0)),
            Box(V(-40.0, -88.0, -32.0), V(40.0, 152.0, 32.0)),
            Box(V(88.0, -88.0, -32.0), V(168.0, 152.0, 32.0)),
            Box(V(-104.0, -152.0, -32.0), V(-24.0, -88.0, 32.0)),
            Box(V(24.0, -152.0, -32.0), V(104.0, -88.0, 32.0)),
        )
    private val eye = V(0.0, 0.0, -600.0)
    private val light = V(-370.0, 415.0, -550.0)

    fun render(yaw: Double): List<String> {
        val matrix = rotation(yaw)
        val origin = inverse(matrix, eye)
        val lamp = inverse(matrix, light)
        return List(HEIGHT) { row ->
            buildString {
                repeat(WIDTH) { col ->
                    val ray = V((col + 0.5 - WIDTH / 2) * 14, (HEIGHT / 2 - row - 0.5) * 28, 600.0).unit()
                    val hit = intersect(origin, inverse(matrix, ray))
                    val value =
                        hit?.let {
                            val diffuse = max(0.0, it.normal.dot((lamp - it.point).unit()))
                            val depth = matrix[2].dot(it.point)
                            (0.22 + 0.72 * diffuse) * (1 - 0.35 * ((depth + 230) / 460).coerceIn(0.0, 1.0))
                        } ?: 0.0
                    append(RAMP[(value * (RAMP.length - 1)).roundToInt().coerceIn(0, RAMP.lastIndex)])
                }
            }
        }
    }

    private fun rotation(yaw: Double): List<V> {
        val cp = cos(Math.toRadians(-18.0))
        val sp = sin(Math.toRadians(-18.0))
        val cr = cos(Math.toRadians(8.0))
        val sr = sin(Math.toRadians(8.0))
        val cy = cos(yaw)
        val sy = sin(yaw)
        return listOf(
            V(cr * cy - sr * sp * sy, -sr * cp, cr * sy + sr * sp * cy),
            V(sr * cy + cr * sp * sy, cr * cp, sr * sy - cr * sp * cy),
            V(-cp * sy, sp, cp * cy),
        )
    }

    private fun inverse(
        m: List<V>,
        v: V,
    ) = V(
        m[0].x * v.x + m[1].x * v.y + m[2].x * v.z,
        m[0].y * v.x + m[1].y * v.y + m[2].y * v.z,
        m[0].z * v.x + m[1].z * v.y + m[2].z * v.z,
    )

    private fun intersect(
        origin: V,
        direction: V,
    ): Hit? = boxes.mapNotNull { hitBox(it, origin, direction) }.minByOrNull { it.distance }

    private fun hitBox(
        box: Box,
        origin: V,
        direction: V,
    ): Hit? {
        var intersects = true
        var near = Double.NEGATIVE_INFINITY
        var far = Double.POSITIVE_INFINITY
        var normal = V(0.0, 0.0, 0.0)
        for (axis in 0..2) {
            val o = origin[axis]
            val d = direction[axis]
            if (abs(d) < 1e-9) {
                intersects = intersects && o >= box.lo[axis] && o <= box.hi[axis]
            } else {
                val a = (box.lo[axis] - o) / d
                val b = (box.hi[axis] - o) / d
                if (min(a, b) > near) {
                    near = min(a, b)
                    val sign = if (a < b) -1.0 else 1.0
                    normal = V(if (axis == 0) sign else 0.0, if (axis == 1) sign else 0.0, if (axis == 2) sign else 0.0)
                }
                far = min(far, max(a, b))
                intersects = intersects && near <= far
            }
        }
        return if (intersects && near > 0) Hit(near, origin + direction * near, normal) else null
    }

    private data class Box(
        val lo: V,
        val hi: V,
    )

    private data class Hit(
        val distance: Double,
        val point: V,
        val normal: V,
    )

    private data class V(
        val x: Double,
        val y: Double,
        val z: Double,
    ) {
        operator fun get(axis: Int): Double =
            when (axis) {
                0 -> x
                1 -> y
                else -> z
            }

        operator fun plus(other: V) = V(x + other.x, y + other.y, z + other.z)

        operator fun minus(other: V) = V(x - other.x, y - other.y, z - other.z)

        operator fun times(k: Double) = V(x * k, y * k, z * k)

        fun dot(other: V) = x * other.x + y * other.y + z * other.z

        fun unit(): V = this * (1 / sqrt(dot(this)))
    }
}
