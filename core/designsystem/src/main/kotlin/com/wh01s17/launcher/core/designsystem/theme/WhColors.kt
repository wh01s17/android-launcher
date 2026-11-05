package com.wh01s17.launcher.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.wh01s17.launcher.core.designsystem.tokens.WhTokens

/** Paleta de `colors.toml` (plan.md §4.2). Los nombres siguen a los del TOML. */
@Immutable
data class WhColors(
    val oledBackground: Color = WhDefaults.oledBackground,
    val background: Color = WhTokens.Colors.background,
    val darkBackground: Color = WhTokens.Colors.darkBackground,
    val darkerBackground: Color = WhTokens.Colors.darkerBackground,
    val lighterBackground: Color = WhTokens.Colors.lighterBackground,
    /** `#243038`: borde inactivo y de controles. */
    val border: Color = WhTokens.Controls.normalBorder,
    val muted: Color = WhTokens.Colors.muted,
    val foreground: Color = WhTokens.Colors.foreground,
    val darkForeground: Color = WhTokens.Colors.darkForeground,
    val lightForeground: Color = WhTokens.Colors.lightForeground,
    val brightForeground: Color = WhTokens.Colors.brightForeground,
    val accent: Color = WhTokens.Colors.accent,
    val selectionBackground: Color = WhTokens.Colors.selectionBackground,
    val selectionForeground: Color = WhTokens.Colors.selectionForeground,
    val red: Color = WhTokens.Colors.red,
    val orange: Color = WhTokens.Colors.orange,
    val yellow: Color = WhTokens.Colors.yellow,
    val green: Color = WhTokens.Colors.green,
    val cyan: Color = WhTokens.Colors.cyan,
    val blue: Color = WhTokens.Colors.blue,
    val magenta: Color = WhTokens.Colors.magenta,
    val brown: Color = WhTokens.Colors.brown,
    val brightRed: Color = WhTokens.Colors.brightRed,
    val brightYellow: Color = WhTokens.Colors.brightYellow,
    val brightGreen: Color = WhTokens.Colors.brightGreen,
    val brightCyan: Color = WhTokens.Colors.brightCyan,
    val brightBlue: Color = WhTokens.Colors.brightBlue,
    val brightMagenta: Color = WhTokens.Colors.brightMagenta,
)
