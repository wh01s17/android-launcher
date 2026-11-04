package com.wh01s17.launcher.core.config

/** Colores de la config: `#RGB`, `#RRGGBB` o `#RRGGBBAA` (como los del shell), a un entero ARGB. */
object HexColor {
    private const val SHORT_LENGTH = 3
    private const val RGB_LENGTH = 6
    private const val RGBA_LENGTH = 8
    private const val OPAQUE = 0xFF000000.toInt()
    private const val HEX_RADIX = 16
    private const val NIBBLE_BITS = 4
    private const val BYTE_BITS = 8
    private const val ALPHA_SHIFT = 24
    private const val BYTE_MASK = 0xFFL

    /** `null` si [text] no es un color válido. */
    fun parse(text: String): Int? {
        val digits = text.removePrefix("#").takeIf { text.startsWith("#") && it.all { c -> c.digitToIntOrNull(HEX_RADIX) != null } }
        return when (digits?.length) {
            SHORT_LENGTH -> OPAQUE or expandShort(digits)
            RGB_LENGTH -> OPAQUE or digits.toInt(HEX_RADIX)
            RGBA_LENGTH -> {
                val value = digits.toLong(HEX_RADIX)
                (((value and BYTE_MASK) shl ALPHA_SHIFT) or (value shr BYTE_BITS)).toInt()
            }
            else -> null
        }
    }

    private fun expandShort(digits: String): Int =
        digits.fold(0) { acc, c ->
            val nibble = c.digitToInt(HEX_RADIX)
            (acc shl BYTE_BITS) or (nibble shl NIBBLE_BITS) or nibble
        }
}
