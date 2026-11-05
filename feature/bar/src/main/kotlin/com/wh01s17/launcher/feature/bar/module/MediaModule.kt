package com.wh01s17.launcher.feature.bar.module

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wh01s17.launcher.core.designsystem.components.WhButton
import com.wh01s17.launcher.core.designsystem.components.WhIcon
import com.wh01s17.launcher.core.designsystem.components.WhPanel
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.icons.WhGlyph
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.media.MediaAction
import com.wh01s17.launcher.core.system.media.MediaSource
import com.wh01s17.launcher.core.system.media.MediaState
import com.wh01s17.launcher.feature.bar.BarPopup
import com.wh01s17.launcher.feature.bar.R

/**
 * `media`: lo que suena ahora, de la app que sea (plan.md §7 Hito 8; `MediaSessionManager`). Sólo
 * ocupa sitio mientras hay una sesión: sin el acceso a notificaciones, que Android exige para verla,
 * o sin nada sonando ni en pausa, no pinta nada. Tocar reproduce o pausa; mantener abre el panel con
 * el título y anterior/siguiente. Los mismos controles tienen `Ctrl + Alt + O/P/I`.
 */
internal class MediaModule(
    private val media: MediaSource,
) : BarModule {
    override val id = ID

    @Composable
    override fun Content(context: BarModuleContext) {
        val sessions = remember(media) { media.active() }
        val state by sessions.collectAsStateWithLifecycle(MediaState.Idle)
        var panelOpen by rememberSaveable { mutableStateOf(false) }
        val session = state as? MediaState.Session ?: return
        val defaults = moduleDefaults(classColors = mapOf(PLAYING to WhTheme.colors.accent), dimClasses = setOf(PAUSED))
        val style = rememberModuleStyle(context.settings, listOf(if (session.playing) PLAYING else PAUSED), defaults)
        val title = session.title ?: stringResource(R.string.bar_media_unknown)
        BarItem(
            style = style,
            contentDescription = stringResource(if (session.playing) R.string.bar_media_playing else R.string.bar_media_paused, title),
            collapsible = !session.playing,
            onClick = { media.perform(session, MediaAction.PlayPause) },
            onLongClick = { panelOpen = true },
        ) {
            // Como un botón de transporte (y el widget de TIDAL): muestra lo que hará tocarlo.
            WhIcon(if (session.playing) WhIcons.Pause else WhIcons.Play)
        }
        if (panelOpen) MediaPanel(media, session, title, onDismiss = { panelOpen = false })
    }

    companion object {
        const val ID = "media"
        const val PLAYING = "playing"
        const val PAUSED = "paused"
    }
}

@Composable
private fun MediaPanel(
    media: MediaSource,
    session: MediaState.Session,
    title: String,
    onDismiss: () -> Unit,
) {
    BarPopup(onDismiss) {
        WhPanel(Modifier.widthIn(max = WhTheme.spacing.menuWidth), contentPadding = WhTheme.spacing.controlGap) {
            WhText(title, style = WhTheme.typography.title, color = WhTheme.colors.accent, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val detail = listOfNotNull(session.artist, session.album).joinToString(" · ")
            if (detail.isNotEmpty()) WhText(detail, color = WhTheme.colors.darkForeground, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
                Key(WhIcons.SkipPrevious, R.string.bar_media_previous) { media.perform(session, MediaAction.Previous) }
                Key(
                    if (session.playing) WhIcons.Pause else WhIcons.Play,
                    if (session.playing) R.string.bar_timer_pause else R.string.bar_timer_start,
                    selected = session.playing,
                ) { media.perform(session, MediaAction.PlayPause) }
                Key(WhIcons.SkipNext, R.string.bar_media_next) { media.perform(session, MediaAction.Next) }
            }
        }
    }
}

@Composable
private fun Key(
    glyph: WhGlyph,
    label: Int,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val description = stringResource(label)
    WhButton(onClick, selected = selected) { WhIcon(glyph, contentDescription = description) }
}
