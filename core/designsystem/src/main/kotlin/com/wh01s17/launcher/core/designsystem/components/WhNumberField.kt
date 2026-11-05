package com.wh01s17.launcher.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

/**
 * Un número que se escribe con el teclado al tocarlo (el tempo del metrónomo): se ve como texto y,
 * al tocarlo, pasa a campo numérico con el valor seleccionado y subrayado. Enter lo aplica; Escape
 * o *atrás* lo cancelan; si el teclado se oculta de otra forma, se aplica lo escrito. [onValue]
 * recibe el número tal cual: ajustarlo a su rango es cosa de quien lo usa.
 */
@Composable
fun WhNumberField(
    value: Int,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = WhTheme.typography.display,
    color: Color = WhTheme.colors.accent,
    enabled: Boolean = true,
    maxDigits: Int = DEFAULT_DIGITS,
    editLabel: String? = null,
) {
    var editing by remember { mutableStateOf(false) }
    if (!editing) {
        val touch = if (enabled) Modifier.clickable(onClickLabel = editLabel, role = Role.Button) { editing = true } else Modifier
        WhText("$value", modifier.then(touch), style = style, color = color, maxLines = 1)
        return
    }
    val text = "$value"
    var field by remember { mutableStateOf(TextFieldValue(text, TextRange(0, text.length))) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val underline = WhTheme.spacing.borderSize
    val finish: (Boolean) -> Unit = { apply ->
        if (apply) field.text.toIntOrNull()?.let(onValue)
        editing = false
        keyboard?.hide()
    }
    BasicTextField(
        value = field,
        onValueChange = { next -> if (next.text.length <= maxDigits && next.text.all(Char::isDigit)) field = next },
        modifier =
            modifier
                .width(IntrinsicSize.Min)
                .focusRequester(focus)
                .onPreviewKeyEvent { event ->
                    val cancel = event.key == Key.Escape || event.key == Key.Back
                    if (cancel) finish(false)
                    cancel
                }.drawBehind {
                    val y = size.height - underline.toPx() / 2
                    drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = underline.toPx())
                },
        textStyle = style.merge(color = color),
        singleLine = true,
        cursorBrush = SolidColor(color),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onDone = { finish(true) }),
    )
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }
    // El teclado se ocultó sin Enter (tocar fuera, el botón de bajarlo): se aplica lo escrito.
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var imeShown by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible) {
        if (imeVisible) {
            imeShown = true
        } else if (imeShown) {
            finish(true)
        }
    }
}

private const val DEFAULT_DIGITS = 3
