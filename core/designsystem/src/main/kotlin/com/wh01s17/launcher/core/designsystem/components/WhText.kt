package com.wh01s17.launcher.core.designsystem.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.wh01s17.launcher.core.designsystem.theme.LocalWhContentColor
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

/** Texto en Departure Mono. El color por defecto lo fija la superficie que lo contiene. */
@Composable
fun WhText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = WhTheme.typography.body,
    color: Color = LocalWhContentColor.current,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(color = color, textAlign = textAlign ?: TextAlign.Unspecified),
        overflow = overflow,
        maxLines = maxLines,
    )
}

/** Variante con estilos por tramo; los tramos sin color usan [color]. */
@Composable
fun WhText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = WhTheme.typography.body,
    color: Color = LocalWhContentColor.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(color = color),
        overflow = overflow,
        maxLines = maxLines,
    )
}
