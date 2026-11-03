package com.wh01s17.launcher.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

/**
 * Velo que oscurece lo que hay detrás de un menú o un popup (`scrim` + `scrim-alpha`).
 * Con [onClick] se cierra al tocarlo; entonces [clickLabel] describe la acción a los lectores
 * de pantalla.
 */
@Composable
fun WhScrim(
    modifier: Modifier = Modifier,
    color: Color = WhTheme.surfaces.menu.scrim,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val tap =
        if (onClick != null) {
            Modifier
                .semantics { clickLabel?.let { contentDescription = it } }
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
        } else {
            Modifier
        }
    Box(modifier.fillMaxSize().background(color).then(tap))
}
