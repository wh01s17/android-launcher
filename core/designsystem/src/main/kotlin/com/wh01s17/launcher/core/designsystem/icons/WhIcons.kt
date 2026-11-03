package com.wh01s17.launcher.core.designsystem.icons

/**
 * Iconos que usa el launcher, en los mismos code points Nerd Font que el shell
 * (plan.md §4.4). Mapa completo y origen: `sources/pixel-icons.py` en los dotfiles.
 * Una prueba comprueba que la fuente trae cada glifo.
 */
object WhIcons {
    private val registry = linkedMapOf<String, WhGlyph>()

    private fun icon(
        name: String,
        codePoint: Int,
    ): WhGlyph = WhGlyph.ofCodePoint(codePoint).also { registry[name] = it }

    /** Nombre → glifo, en orden de declaración (galería y pruebas). */
    internal val catalog: Map<String, WhGlyph> get() = registry

    // Sistema
    val Apps = icon("Apps", 0xF003B)
    val Menu = icon("Menu", 0xF035C)
    val Power = icon("Power", 0xF011)
    val PowerOff = icon("PowerOff", 0xF0425)
    val Reboot = icon("Reboot", 0xF0709)
    val Logout = icon("Logout", 0xF0343)
    val Suspend = icon("Suspend", 0xF04B2)
    val Lock = icon("Lock", 0xF023)
    val Unlock = icon("Unlock", 0xF07F5)
    val Settings = icon("Settings", 0xE615)
    val Info = icon("Info", 0xEA74)
    val Refresh = icon("Refresh", 0xF021)
    val Terminal = icon("Terminal", 0xF489)
    val Keyboard = icon("Keyboard", 0xF11C)
    val Monitor = icon("Monitor", 0xF0379)
    val Theme = icon("Theme", 0xF0E0C)
    val Font = icon("Font", 0xE659)
    val Image = icon("Image", 0xF03E)
    val Layout = icon("Layout", 0xE62A)
    val Hyprland = icon("Hyprland", 0xF359)
    val Android = icon("Android", 0xF0032)

    // Navegación y edición
    val Check = icon("Check", 0xF00C)
    val Close = icon("Close", 0xF0156)
    val Plus = icon("Plus", 0xF0415)
    val Pencil = icon("Pencil", 0xF040)
    val Trash = icon("Trash", 0xF01B4)
    val Star = icon("Star", 0xF005)
    val Eye = icon("Eye", 0xF06E)
    val EyeOff = icon("EyeOff", 0xF0209)
    val ChevronLeft = icon("ChevronLeft", 0xF0141)
    val ChevronRight = icon("ChevronRight", 0xF0142)
    val ChevronDown = icon("ChevronDown", 0xF0140)
    val ArrowUp = icon("ArrowUp", 0xF005D)
    val ArrowDown = icon("ArrowDown", 0xF0045)
    val ArrowLeft = icon("ArrowLeft", 0xF004D)
    val ArrowRight = icon("ArrowRight", 0xF0054)
    val MoreHorizontal = icon("MoreHorizontal", 0xF01D8)
    val ExternalLink = icon("ExternalLink", 0xF03CC)
    val Folder = icon("Folder", 0xF024B)
    val File = icon("File", 0xF0214)
    val Clipboard = icon("Clipboard", 0xF07F)

    // Estado
    val Wifi = icon("Wifi", 0xF0928)
    val Bluetooth = icon("Bluetooth", 0xF00AF)
    val Battery = icon("Battery", 0xF0079)
    val BatteryCharging = icon("BatteryCharging", 0xF0085)
    val Bell = icon("Bell", 0xF009A)
    val BellOff = icon("BellOff", 0xF009B)
    val Clock = icon("Clock", 0xE383)
    val Calendar = icon("Calendar", 0xF00ED)
    val VolumeHigh = icon("VolumeHigh", 0xF057E)
    val VolumeLow = icon("VolumeLow", 0xF027)
    val VolumeMute = icon("VolumeMute", 0xF075F)
    val Mic = icon("Mic", 0xF036C)

    // Medios
    val Play = icon("Play", 0xF040A)
    val Pause = icon("Pause", 0xF03E4)
    val SkipNext = icon("SkipNext", 0xF04AD)
    val SkipPrevious = icon("SkipPrevious", 0xF04AE)
    val Music = icon("Music", 0xF075A)
}
