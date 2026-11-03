package com.wh01s17.launcher.core.designsystem.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wh01s17.launcher.core.designsystem.components.WhButton
import com.wh01s17.launcher.core.designsystem.components.WhIcon
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.components.WhTextField
import com.wh01s17.launcher.core.designsystem.components.WhToggle
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.theme.WhControlState
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GalleryButtons() {
    GallerySection("controls") {
        Column(verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                WhControlState.entries.forEach { state ->
                    WhButton(onClick = {}, selected = state == WhControlState.Selected, interactionSource = forcedSource(state)) {
                        WhText(state.name)
                    }
                }
                WhButton(onClick = {}, enabled = false) { WhText("Disabled") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                WhButton(onClick = {}) {
                    WhIcon(WhIcons.Apps)
                    WhText("Apps")
                }
                WhButton(onClick = {}) {
                    WhIcon(WhIcons.Power)
                    WhText("System")
                }
                WhButton(onClick = {}, selected = true) {
                    WhIcon(WhIcons.Settings)
                    WhText("Setup")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.panelPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var on by remember { mutableStateOf(true) }
                var off by remember { mutableStateOf(false) }
                WhToggle(checked = on, onCheckedChange = { on = it }, label = "On")
                WhToggle(checked = off, onCheckedChange = { off = it }, label = "Off")
                WhToggle(checked = false, onCheckedChange = {}, label = "Disabled", enabled = false)
            }
        }
    }
}

@Composable
internal fun GalleryFields() {
    GallerySection("text fields") {
        Column(verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
            var typed by remember { mutableStateOf("") }
            WhTextField(typed, { typed = it }, Modifier.fillMaxWidth(), placeholder = "Search apps...", leadingIcon = WhIcons.Apps)
            WhTextField("wh01s17", {}, Modifier.fillMaxWidth(), leadingIcon = WhIcons.Terminal)
            WhTextField(
                value = "focused",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                interactionSource = forcedSource(WhControlState.Focus),
            )
        }
    }
}
