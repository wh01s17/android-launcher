package com.wh01s17.launcher.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import com.wh01s17.launcher.core.designsystem.icons.WhGlyph
import com.wh01s17.launcher.core.designsystem.theme.LocalWhContentColor
import com.wh01s17.launcher.core.designsystem.theme.WhFonts
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

/**
 * Icono pixel. El tamaño se da en dp y no en sp: un icono no debe crecer con la fuente del
 * sistema. Con múltiplos de 16 dp a 480 dpi cada píxel del icono cae en píxeles enteros.
 *
 * Sin [contentDescription] el icono es decorativo y se oculta a los lectores de pantalla
 * (si no, leerían el code point privado del glifo).
 */
@Composable
fun WhIcon(
    glyph: WhGlyph,
    modifier: Modifier = Modifier,
    size: Dp = WhTheme.spacing.iconSize,
    tint: Color = LocalWhContentColor.current,
    contentDescription: String? = null,
) {
    val fontSize = with(LocalDensity.current) { size.toSp() }
    val semantics =
        if (contentDescription != null) {
            Modifier.semantics { this.contentDescription = contentDescription }
        } else {
            Modifier.clearAndSetSemantics { }
        }
    Box(modifier.size(size).then(semantics), contentAlignment = Alignment.Center) {
        BasicText(
            text = glyph.text,
            style =
                TextStyle(
                    fontFamily = WhFonts.pixelIcons,
                    fontSize = fontSize,
                    lineHeight = fontSize,
                    color = tint,
                    fontSynthesis = FontSynthesis.None,
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                ),
            softWrap = false,
            maxLines = 1,
        )
    }
}
