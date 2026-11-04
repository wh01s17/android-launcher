package com.wh01s17.launcher.feature.bar

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.config.BarConfig
import com.wh01s17.launcher.core.config.BarModuleConfig
import com.wh01s17.launcher.core.designsystem.components.WhIcon
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.modifier.whBorder
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.feature.bar.module.BarItem
import com.wh01s17.launcher.feature.bar.module.BarModuleContext
import com.wh01s17.launcher.feature.bar.module.BarModuleRegistry
import com.wh01s17.launcher.feature.bar.module.ModuleStyle

/**
 * La barra superior (plan.md §5.5): tres grupos de módulos de la config sobre el fondo del
 * launcher, con contorno si `outline` y sin fondo propio si `transparent`. Deslizar hacia abajo
 * emite [LauncherCommand.OpenNotificationShade]. El alto es el de la barra del tema, con el área
 * táctil de 48 dp como mínimo. Quien la coloca le aplica los *insets* de la barra de estado.
 */
@Composable
fun Bar(
    config: BarConfig,
    registry: BarModuleRegistry,
    state: BarState,
    onCommand: (LauncherCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = WhTheme.spacing
    val surface = WhTheme.surfaces.bar.surface
    val shape = WhTheme.borders.shape
    val height = maxOf(spacing.barHeight, spacing.touchTarget)
    val dragThreshold = with(LocalDensity.current) { height.toPx() }
    val groupGapPx = with(LocalDensity.current) { (spacing.barItemPaddingX * 2).roundToPx() }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (config.outline) Modifier.padding(horizontal = spacing.gapsOut) else Modifier)
                .then(if (config.transparent) Modifier else Modifier.background(surface.background, shape))
                .then(
                    if (config.outline) {
                        Modifier.whBorder(
                            active = true,
                            shape = shape,
                            width = surface.borderWidth,
                            alpha = surface.borderAlpha,
                        )
                    } else {
                        Modifier
                    },
                ).swipeDownToOpenShade(dragThreshold) { onCommand(LauncherCommand.OpenNotificationShade) }
                .padding(horizontal = spacing.controlGap),
    ) {
        BarLayout(itemGapPx = 0, groupGapPx = groupGapPx, modifier = Modifier.fillMaxWidth().height(height)) {
            BarContent(config, registry, state, onCommand)
        }
    }
}

@Composable
private fun BarContent(
    config: BarConfig,
    registry: BarModuleRegistry,
    state: BarState,
    onCommand: (LauncherCommand) -> Unit,
) {
    val layout = config.layout
    for (notice in state.notices) {
        Box(Modifier.barSlot(BarSlot(BarGroup.Right))) { NoticeItem(notice) }
    }
    for ((group, modules) in listOf(BarGroup.Left to layout.left, BarGroup.Center to layout.center, BarGroup.Right to layout.right)) {
        for (module in modules) {
            val slot = BarSlot(group, anchor = group == BarGroup.Center && module.id == config.centerAnchor)
            Box(Modifier.barSlot(slot)) { ModuleHost(module, registry, state, onCommand) }
        }
    }
}

/** Pinta un módulo de la config; si su `id` no existe, un aviso amarillo en vez de nada. */
@Composable
private fun ModuleHost(
    settings: BarModuleConfig,
    registry: BarModuleRegistry,
    state: BarState,
    onCommand: (LauncherCommand) -> Unit,
) {
    val module = registry.find(settings.id)
    if (module != null) {
        module.Content(BarModuleContext(settings, state, onCommand))
    } else {
        val colors = WhTheme.colors
        BarItem(
            ModuleStyle(colors.yellow, dimmed = true, brackets = true, bracketColor = colors.muted),
            contentDescription = stringResource(R.string.bar_unknown_module, settings.id),
        ) {
            WhText("?${settings.id}", maxLines = 1)
        }
    }
}

/**
 * Un aviso de la barra (config inválida, Xiaomi rendido): un icono rojo o amarillo, para que quepa
 * junto al resto de módulos en 407 dp. El texto lo lee TalkBack.
 */
@Composable
private fun NoticeItem(notice: BarNotice) {
    val colors = WhTheme.colors
    val color = if (notice.severity == NoticeSeverity.Error) colors.red else colors.yellow
    BarItem(ModuleStyle(color, dimmed = false, brackets = false, bracketColor = colors.muted), contentDescription = notice.text) {
        WhIcon(WhIcons.Info)
    }
}

/** Un arrastre hacia abajo de [threshold] píxeles, una sola vez por gesto (plan.md §3). */
@Composable
private fun Modifier.swipeDownToOpenShade(
    threshold: Float,
    onSwipe: () -> Unit,
): Modifier {
    // El gesto vive más que una composición: se lee siempre el `onSwipe` más reciente.
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    return pointerInput(threshold) {
        var travelled = 0f
        var fired = false
        detectVerticalDragGestures(
            onDragStart = {
                travelled = 0f
                fired = false
            },
            onVerticalDrag = { change, dragAmount ->
                travelled += dragAmount
                if (!fired && travelled > threshold) {
                    fired = true
                    change.consume()
                    currentOnSwipe()
                }
            },
        )
    }
}
