package com.wh01s17.launcher.feature.settings.style

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wh01s17.launcher.core.designsystem.components.WhListRow
import com.wh01s17.launcher.core.designsystem.components.WhPanel
import com.wh01s17.launcher.core.designsystem.components.WhScrim
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.layout.chrome
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.settings.R

/**
 * Una opción de [ChoiceSetting]: [id] es lo que se guarda en la config; [title], lo que se lee;
 * [description] va debajo y [swatch], unas muestras de color delante (las paletas).
 */
internal class Choice(
    val id: String,
    val title: String,
    val description: String? = null,
    val swatch: List<Color> = emptyList(),
)

/**
 * Una fila «[label] actual ›» (con `%1$s` donde va el título de la elegida) que abre un panel con todas
 * las [choices]. Elegir una la aplica y cierra; atrás o tocar fuera cierran sin cambiar nada. Todas las
 * elecciones de una lista en Estilo usan esta misma forma.
 */
@Composable
internal fun ChoiceSetting(
    label: Int,
    title: Int,
    choices: List<Choice>,
    current: String,
    onSelect: (String) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val selected = choices.firstOrNull { it.id == current } ?: choices.first()
    WhListRow(
        title = stringResource(label, selected.title),
        onClick = { open = true },
        modifier = Modifier.fillMaxWidth(),
        chevron = true,
    )
    if (open) ChoiceDialog(title, choices, selected.id, { onSelect(it) }) { open = false }
}

@Composable
private fun ChoiceDialog(
    title: Int,
    choices: List<Choice>,
    current: String,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    val spacing = WhTheme.spacing
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            WhScrim(onClick = onClose, clickLabel = stringResource(R.string.style_choice_close))
            WhPanel(
                Modifier
                    .windowInsetsPadding(WindowInsets.chrome)
                    .padding(spacing.panelPadding)
                    .widthIn(max = spacing.menuWidth),
            ) {
                WhText(stringResource(title), style = WhTheme.typography.title, color = WhTheme.colors.accent)
                // Con muchas opciones (las 16 paletas), la lista se desplaza dentro del panel.
                Column(
                    Modifier.heightIn(max = spacing.menuWidth * 2).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(spacing.controlGap),
                ) {
                    choices.forEach { choice ->
                        ChoiceRow(choice, choice.id == current) {
                            onSelect(choice.id)
                            onClose()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    choice: Choice,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = WhTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.controlGap / 2)) {
        WhListRow(
            title = AnnotatedString(choice.title),
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            selected = selected,
            leading =
                if (choice.swatch.isEmpty()) {
                    null
                } else {
                    { _ -> Row { choice.swatch.forEach { Box(Modifier.size(spacing.iconSize).background(it)) } } }
                },
        )
        choice.description?.let {
            WhText(it, style = WhTheme.typography.bodySmall, color = WhTheme.colors.darkForeground)
        }
    }
}
