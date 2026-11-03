package com.wh01s17.launcher.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.wh01s17.launcher.core.designsystem.tokens.WhTokens

/**
 * Escala tipográfica de `shell.font.toml` (plan.md §4.4).
 *
 * Los tamaños del shell son píxeles de escritorio; en el teléfono se multiplican por
 * `fontScale` (1,25 por defecto, ver [WhDefaults.FONT_SCALE]) para que se lean bien.
 * Sin cursivas ni negritas: Departure Mono sólo tiene Regular, así que se desactiva la
 * síntesis de pesos.
 */
@Immutable
data class WhTypography(
    val caption: TextStyle,
    val bodySmall: TextStyle,
    val body: TextStyle,
    val subtitle: TextStyle,
    val title: TextStyle,
    val heading: TextStyle,
    val display: TextStyle,
    val displayLarge: TextStyle,
) {
    companion object {
        private const val LINE_HEIGHT_RATIO = 1.4f

        fun create(fontScale: Float): WhTypography =
            WhTypography(
                caption = style(WhTokens.Font.caption, fontScale),
                bodySmall = style(WhTokens.Font.bodySmall, fontScale),
                body = style(WhTokens.Font.body, fontScale),
                subtitle = style(WhTokens.Font.subtitle, fontScale),
                title = style(WhTokens.Font.title, fontScale),
                heading = style(WhTokens.Font.heading, fontScale),
                display = style(WhTokens.Font.display, fontScale),
                displayLarge = style(WhTokens.Font.displayLarge, fontScale),
            )

        private fun style(
            size: TextUnit,
            fontScale: Float,
        ): TextStyle {
            val scaled = (size.value * fontScale).sp
            return TextStyle(
                fontFamily = WhFonts.departureMono,
                fontWeight = FontWeight.Normal,
                fontSynthesis = FontSynthesis.None,
                fontSize = scaled,
                lineHeight = (scaled.value * LINE_HEIGHT_RATIO).sp,
                letterSpacing = 0.sp,
            )
        }
    }
}
