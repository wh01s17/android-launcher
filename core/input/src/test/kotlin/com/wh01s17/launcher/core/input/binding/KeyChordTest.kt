package com.wh01s17.launcher.core.input.binding

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyChordTest {
    private fun valid(text: String) = (KeyChord.parse(text) as ChordParse.Valid).chord

    private fun invalid(text: String) = (KeyChord.parse(text) as ChordParse.Invalid).message

    @Test
    fun `chords are read like hyprland, ignoring case, spaces and aliases`() {
        assertEquals(KeyChord(KeyEvent.KEYCODE_1, setOf(KeyModifier.Super, KeyModifier.Shift)), valid("SUPER + SHIFT + 1"))
        assertEquals(KeyChord(KeyEvent.KEYCODE_SPACE, setOf(KeyModifier.Super)), valid("super+space"))
        assertEquals(KeyChord(KeyEvent.KEYCODE_ENTER, setOf(KeyModifier.Super, KeyModifier.Ctrl)), valid("mod + control + enter"))
        assertEquals(KeyChord(KeyEvent.KEYCODE_F12), valid("F12"))
        assertEquals(KeyChord(KeyEvent.KEYCODE_Z, setOf(KeyModifier.Alt)), valid(" ALT +z "))
    }

    @Test
    fun `chords are written with modifiers in a fixed order and canonical key names`() {
        assertEquals("SUPER + CTRL + SHIFT + RETURN", valid("shift + ctrl + super + enter").toString())
        assertEquals("SUPER + EQUAL", valid("SUPER + EQUALS").toString())
    }

    @Test
    fun `bad chords explain what is wrong`() {
        assertEquals("«SUPER + » no es una combinación; ejemplo: «SUPER + SHIFT + 1»", invalid("SUPER + "))
        assertEquals("«HYPER» no es un modificador; opciones: SUPER, CTRL, ALT, SHIFT", invalid("HYPER + 1"))
        assertEquals("sólo la última parte es la tecla («B»); «A» no es un modificador", invalid("A + B"))
        assertEquals("«SUPER» aparece dos veces", invalid("SUPER + MOD + 1"))
        assertEquals("falta la tecla después de «SHIFT»", invalid("SUPER + SHIFT"))
        assertEquals("«KEY» no es una tecla conocida", invalid("SUPER + KEY"))
    }

    @Test
    fun `events give their chord, with numpad keys as the normal ones and modifiers alone ignored`() {
        val superShift = KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON or KeyEvent.META_SHIFT_ON
        assertEquals(valid("SUPER + SHIFT + 3"), KeyChord.fromEvent(KeyEvent.KEYCODE_NUMPAD_3, superShift))
        assertEquals(valid("CTRL + ALT + O"), KeyChord.fromEvent(KeyEvent.KEYCODE_O, KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON))
        assertEquals(valid("RETURN"), KeyChord.fromEvent(KeyEvent.KEYCODE_NUMPAD_ENTER, 0))
        assertNull(KeyChord.fromEvent(KeyEvent.KEYCODE_META_LEFT, KeyEvent.META_META_ON))
        assertNull(KeyChord.fromEvent(KeyEvent.KEYCODE_UNKNOWN, 0))
    }
}
