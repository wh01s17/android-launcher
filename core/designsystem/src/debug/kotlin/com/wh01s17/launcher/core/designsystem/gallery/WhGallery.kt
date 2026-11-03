package com.wh01s17.launcher.core.designsystem.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhControlState
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Galería de componentes (sólo debug, plan.md §7 Hito 1): todos los componentes en todos sus
 * estados. Sirve para revisar el tema a ojo y como referencia de las capturas Roborazzi.
 */
@Composable
fun WhGallery(modifier: Modifier = Modifier) {
    WhTheme {
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(WhTheme.colors.background)
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(WhTheme.spacing.panelPadding),
            verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.panelPadding),
        ) {
            GalleryColors()
            GalleryTypography()
            GalleryIcons()
            GalleryButtons()
            GalleryFields()
            GalleryPanels()
            GalleryLists()
            GalleryBar()
        }
    }
}

/** Encabezado de sección con el mismo trazo que usa el shell para separar bloques. */
@Composable
internal fun GallerySection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.panelGap)) {
        WhText("-- $title --", style = WhTheme.typography.caption, color = WhTheme.colors.darkForeground)
        content()
    }
}

/**
 * Fuente de interacciones que ya está en [state], para mostrar un estado de forma estática
 * pasando por el mismo camino que un toque real (no se dibuja un estado falso).
 *
 * Un `MutableInteractionSource` normal no repite lo emitido: si la interacción se emite antes de
 * que el control empiece a escuchar, se pierde. Esta fuente la entrega a cada colector al suscribirse.
 */
@Composable
internal fun forcedSource(state: WhControlState): MutableInteractionSource =
    remember(state) { ForcedInteractionSource(state.interaction()) }

private fun WhControlState.interaction(): Interaction? =
    when (this) {
        WhControlState.Hover -> HoverInteraction.Enter()
        WhControlState.Focus -> FocusInteraction.Focus()
        WhControlState.Pressed -> PressInteraction.Press(Offset.Zero)
        WhControlState.Normal, WhControlState.Selected -> null
    }

private class ForcedInteractionSource(
    private val forced: Interaction?,
) : MutableInteractionSource {
    private val delegate = MutableInteractionSource()
    override val interactions: Flow<Interaction> =
        flow {
            forced?.let { emit(it) }
            emitAll(delegate.interactions)
        }

    override suspend fun emit(interaction: Interaction) = delegate.emit(interaction)

    override fun tryEmit(interaction: Interaction): Boolean = delegate.tryEmit(interaction)
}
