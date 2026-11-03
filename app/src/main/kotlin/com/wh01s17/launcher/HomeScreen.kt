package com.wh01s17.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

/**
 * Inicio provisional: fondo wh01s17 y una pista. Deslizar hacia arriba o tocar abre el drawer
 * (plan.md §3: "deslizar hacia arriba → menú"). Los workspaces la sustituyen en el Hito 4.
 */
@Composable
internal fun HomeScreen(
    onOpenApps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Un desplazamiento hacia arriba de una fila de menú basta para abrir; el ruido no.
    val threshold = with(LocalDensity.current) { WhTheme.spacing.menuRowHeight.toPx() }
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(WhTheme.colors.background)
                .clickable(interaction, indication = null, onClickLabel = stringResource(R.string.home_open_apps), onClick = onOpenApps)
                .pointerInput(onOpenApps, threshold) {
                    var travelled = 0f
                    detectVerticalDragGestures(
                        onDragStart = { travelled = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            travelled += dragAmount
                            if (travelled < -threshold) {
                                change.consume()
                                travelled = 0f
                                onOpenApps()
                            }
                        },
                    )
                }.windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.BottomCenter,
    ) {
        WhText(
            stringResource(R.string.home_hint),
            Modifier.padding(WhTheme.spacing.panelPadding),
            style = WhTheme.typography.caption,
            color = WhTheme.colors.darkForeground,
        )
    }
}
