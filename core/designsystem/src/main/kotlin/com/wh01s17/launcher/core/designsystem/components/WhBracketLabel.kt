package com.wh01s17.launcher.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.wh01s17.launcher.core.designsystem.theme.LocalWhContentColor
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

/**
 * Módulo de la barra entre corchetes, `[ texto ]`, con los corchetes en `muted`
 * (`brackets: true` de `StatusModule.qml`, plan.md §4.7).
 */
@Composable
fun WhBracketLabel(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = WhTheme.typography.body,
    color: Color = LocalWhContentColor.current,
    bracketColor: Color = WhTheme.colors.muted,
) {
    val annotated =
        buildAnnotatedString {
            withStyle(SpanStyle(color = bracketColor)) { append("[ ") }
            withStyle(SpanStyle(color = color)) { append(text) }
            withStyle(SpanStyle(color = bracketColor)) { append(" ]") }
        }
    WhText(text = annotated, modifier = modifier, style = style, color = color)
}
