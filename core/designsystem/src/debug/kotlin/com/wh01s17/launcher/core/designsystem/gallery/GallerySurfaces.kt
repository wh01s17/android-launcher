package com.wh01s17.launcher.core.designsystem.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.wh01s17.launcher.core.designsystem.components.WhBracketLabel
import com.wh01s17.launcher.core.designsystem.components.WhListRow
import com.wh01s17.launcher.core.designsystem.components.WhListRowSize
import com.wh01s17.launcher.core.designsystem.components.WhPanel
import com.wh01s17.launcher.core.designsystem.components.WhScrim
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.components.WhTooltip
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.theme.WhControlState
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

@Composable
internal fun GalleryPanels() {
    GallerySection("panels, scrim, tooltip") {
        Column(verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.panelPadding)) {
            Row(horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.panelGap)) {
                WhPanel(Modifier.weight(1f), active = true) {
                    WhText("active", color = WhTheme.colors.accent)
                    WhText("gradient border", style = WhTheme.typography.bodySmall, color = WhTheme.colors.darkForeground)
                }
                WhPanel(Modifier.weight(1f), active = false) {
                    WhText("inactive")
                    WhText("flat border", style = WhTheme.typography.bodySmall, color = WhTheme.colors.darkForeground)
                }
            }
            // Un velo sobre contenido: se ve cuánto deja pasar.
            Box(Modifier.fillMaxWidth().height(160.dp).clip(WhTheme.borders.shape)) {
                Column(Modifier.fillMaxSize().background(WhTheme.colors.lighterBackground).padding(WhTheme.spacing.panelGap)) {
                    repeat(6) { WhText("content behind the scrim", color = WhTheme.colors.lightForeground) }
                }
                WhScrim()
                WhPanel(Modifier.align(Alignment.Center), style = WhTheme.surfaces.popup, contentPadding = WhTheme.spacing.popupPadding) {
                    WhText("popup over scrim")
                }
            }
            WhTooltip("Super + Space: menu")
        }
    }
}

@Composable
internal fun GalleryMenu() {
    GallerySection("menu - Menu.qml") {
        // Como el menú del shell (Super+Space): ancho de 300 px, filas de 50 px y `›` en los submenús.
        WhPanel(Modifier.width(WhTheme.spacing.menuWidth), contentPadding = WhTheme.spacing.controlGap) {
            Column {
                WhText(
                    "Go\u2026",
                    Modifier.padding(horizontal = WhTheme.spacing.rowPaddingX),
                    style = WhTheme.typography.heading,
                    color = WhTheme.colors.darkForeground,
                )
                WhListRow("Apps", {}, leadingIcon = WhIcons.Apps, chevron = true, size = WhListRowSize.Menu, selected = true)
                WhListRow("Style", {}, leadingIcon = WhIcons.Theme, chevron = true, size = WhListRowSize.Menu)
                WhListRow(
                    "Setup",
                    {},
                    leadingIcon = WhIcons.Settings,
                    chevron = true,
                    size = WhListRowSize.Menu,
                    interactionSource = forcedSource(WhControlState.Focus),
                )
                WhListRow("About", {}, leadingIcon = WhIcons.Info, size = WhListRowSize.Menu)
            }
        }
    }
}

@Composable
internal fun GalleryLists() {
    GallerySection("list rows - popups") {
        WhPanel(Modifier.fillMaxWidth(), style = WhTheme.surfaces.popup, contentPadding = WhTheme.spacing.controlGap) {
            // Las filas van juntas: el hueco de WhPanel es para secciones, no para listas.
            Column {
                WhListRow("Apps", {}, leadingIcon = WhIcons.Apps, trailingText = "Super+Alt+Space")
                WhListRow("Style", {}, leadingIcon = WhIcons.Theme, selected = true)
                WhListRow(
                    "System",
                    {},
                    leadingIcon = WhIcons.Power,
                    trailingText = "Super+Esc",
                    interactionSource = forcedSource(WhControlState.Pressed),
                )
                WhListRow("Disabled", {}, leadingIcon = WhIcons.Lock, enabled = false)
            }
        }
    }
}

@Composable
internal fun GalleryBar() {
    val c = WhTheme.colors
    GallerySection("bar modules - brackets") {
        Row(horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap), verticalAlignment = Alignment.CenterVertically) {
            WhBracketLabel("main", color = c.accent)
            WhBracketLabel("dirty", color = c.yellow)
            WhBracketLabel("fault", color = c.red)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap), verticalAlignment = Alignment.CenterVertically) {
            WhBracketLabel("192.168.3.54", color = c.accent)
            WhBracketLabel("40:00", color = c.red)
            WhBracketLabel("-", color = c.darkForeground)
        }
    }
}
