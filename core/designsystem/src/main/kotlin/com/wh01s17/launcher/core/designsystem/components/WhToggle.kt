package com.wh01s17.launcher.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.wh01s17.launcher.core.designsystem.modifier.rememberWhControlState
import com.wh01s17.launcher.core.designsystem.modifier.whControlSurface
import com.wh01s17.launcher.core.designsystem.modifier.whTouchTarget
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

private val TrackWidth = 40.dp
private val TrackHeight = 22.dp
private val Thumb = 12.dp
private val ThumbInset = 4.dp
private const val DISABLED_ALPHA = 0.4f

/**
 * Interruptor. Encendido usa el estilo *selected* (verde); apagado, el normal. La etiqueta
 * es opcional y forma parte del área táctil.
 */
@Composable
fun WhToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val state = rememberWhControlState(interactionSource, selected = checked, enabled = enabled)
    val style = WhTheme.controls.style(state)
    val colors = WhTheme.colors
    val thumbColor = if (checked) colors.accent else colors.darkForeground
    val shape = WhTheme.borders.shape
    Row(
        modifier =
            modifier
                .whTouchTarget()
                .toggleable(
                    value = checked,
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ),
        horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(TrackWidth, TrackHeight).whControlSurface(state, shape),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .padding(horizontal = ThumbInset)
                    .size(Thumb)
                    .background(if (enabled) thumbColor else thumbColor.copy(alpha = DISABLED_ALPHA), shape),
            )
        }
        if (label != null) {
            WhText(label, color = if (enabled) style.content else style.content.copy(alpha = DISABLED_ALPHA))
        }
    }
}
