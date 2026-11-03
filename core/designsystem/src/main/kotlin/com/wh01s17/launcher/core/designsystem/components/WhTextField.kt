package com.wh01s17.launcher.core.designsystem.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import com.wh01s17.launcher.core.designsystem.icons.WhGlyph
import com.wh01s17.launcher.core.designsystem.modifier.rememberWhControlState
import com.wh01s17.launcher.core.designsystem.modifier.whControlSurface
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

/**
 * Campo de texto. Sin foco usa el estilo normal de los controles y con foco el cian
 * (`focus-*`). Mide 48 dp de alto, porque aquí el área de toque es el propio dibujo.
 */
@Composable
fun WhTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: WhGlyph? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val state = rememberWhControlState(interactionSource, enabled = enabled)
    val spacing = WhTheme.spacing
    val colors = WhTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        textStyle = WhTheme.typography.body.merge(color = colors.foreground),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        decorationBox = { inner ->
            Row(
                modifier =
                    Modifier
                        .whControlSurface(state)
                        .defaultMinSize(minHeight = spacing.touchTarget)
                        .padding(horizontal = spacing.controlPaddingX),
                horizontalArrangement = Arrangement.spacedBy(spacing.controlGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) WhIcon(leadingIcon, tint = colors.darkForeground)
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty() && placeholder != null) {
                        WhText(placeholder, color = colors.darkForeground, maxLines = 1)
                    }
                    inner()
                }
            }
        },
    )
}
