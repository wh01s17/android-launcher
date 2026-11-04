package com.wh01s17.launcher.core.input.binding

import android.view.KeyEvent

/** Modificadores de un atajo, con el nombre que usan `bindings.lua` y `launcher.json`. */
enum class KeyModifier(
    val configName: String,
    internal val metaFlag: Int,
) {
    Super("SUPER", KeyEvent.META_META_ON),
    Ctrl("CTRL", KeyEvent.META_CTRL_ON),
    Alt("ALT", KeyEvent.META_ALT_ON),
    Shift("SHIFT", KeyEvent.META_SHIFT_ON),
    ;

    internal companion object {
        /** Otros nombres habituales (`MOD`, `META`, `CONTROL`…), en mayúsculas. */
        val ALIASES: Map<String, KeyModifier> =
            entries.associateBy { it.configName } +
                mapOf("MOD" to Super, "META" to Super, "WIN" to Super, "CONTROL" to Ctrl)
    }
}

/**
 * Una combinación de teclas: una tecla ([keyCode], de `KeyEvent`) y los modificadores que deben
 * estar pulsados, ni más ni menos. Se escribe como en Hyprland: `SUPER + SHIFT + 1`.
 */
data class KeyChord(
    val keyCode: Int,
    val modifiers: Set<KeyModifier> = emptySet(),
) {
    /** Con modificadores en el orden fijo de [KeyModifier], para la tabla de atajos y los errores. */
    override fun toString(): String =
        (KeyModifier.entries.filter { it in modifiers }.map { it.configName } + KeyNames.nameOf(keyCode)).joinToString(" + ")

    companion object {
        /** `SUPER + SHIFT + 1` → la combinación; mayúsculas y espacios dan igual. */
        fun parse(text: String): ChordParse {
            val parts = text.split('+').map { it.trim().uppercase() }
            if (parts.any { it.isEmpty() }) return ChordParse.Invalid("«$text» no es una combinación; ejemplo: «SUPER + SHIFT + 1»")
            val modifiers = mutableSetOf<KeyModifier>()
            for (name in parts.dropLast(1)) {
                val modifier = KeyModifier.ALIASES[name] ?: return ChordParse.Invalid(unknownModifier(name, parts.last()))
                if (!modifiers.add(modifier)) return ChordParse.Invalid("«${modifier.configName}» aparece dos veces")
            }
            val key = parts.last()
            if (key in KeyModifier.ALIASES) return ChordParse.Invalid("falta la tecla después de «$key»")
            val keyCode = KeyNames.codeOf(key) ?: return ChordParse.Invalid("«$key» no es una tecla conocida")
            return ChordParse.Valid(KeyChord(keyCode, modifiers))
        }

        /**
         * La combinación de una pulsación, o `null` si la tecla es un modificador (pulsar `Super` solo
         * no es un atajo) o no tiene nombre. Las teclas del teclado numérico cuentan como las normales.
         */
        fun fromEvent(
            keyCode: Int,
            metaState: Int,
        ): KeyChord? {
            val key = KeyNames.normalize(keyCode)
            if (KeyNames.isModifier(key) || KeyNames.nameOf(key) == null) return null
            return KeyChord(key, KeyModifier.entries.filterTo(mutableSetOf()) { metaState and it.metaFlag != 0 })
        }

        private fun unknownModifier(
            name: String,
            last: String,
        ): String =
            if (KeyNames.codeOf(name) != null) {
                "sólo la última parte es la tecla («$last»); «$name» no es un modificador"
            } else {
                "«$name» no es un modificador; opciones: ${KeyModifier.entries.joinToString { it.configName }}"
            }
    }
}

sealed interface ChordParse {
    data class Valid(
        val chord: KeyChord,
    ) : ChordParse

    data class Invalid(
        val message: String,
    ) : ChordParse
}
