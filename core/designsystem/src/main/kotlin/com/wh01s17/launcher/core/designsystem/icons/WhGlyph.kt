package com.wh01s17.launcher.core.designsystem.icons

import androidx.compose.runtime.Immutable

/**
 * Un glifo de `wh01s17 Pixel Icons`. Varios code points están fuera del plano básico
 * (por encima de U+FFFF), por eso se guarda como `String` y no como `Char`.
 */
@Immutable
@JvmInline
value class WhGlyph(
    val text: String,
) {
    companion object {
        fun ofCodePoint(codePoint: Int): WhGlyph = WhGlyph(String(Character.toChars(codePoint)))
    }
}
