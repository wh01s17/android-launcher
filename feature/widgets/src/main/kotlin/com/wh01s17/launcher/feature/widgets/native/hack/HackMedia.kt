package com.wh01s17.launcher.feature.widgets.native.hack

import android.content.ActivityNotFoundException
import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.layout.WidgetSize
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.media.MediaAction
import com.wh01s17.launcher.core.system.media.MediaSource
import com.wh01s17.launcher.core.system.media.MediaState
import com.wh01s17.launcher.feature.widgets.R
import com.wh01s17.launcher.feature.widgets.native.NativeWidgetSources
import com.wh01s17.launcher.feature.widgets.native.TIDAL_PACKAGE
import com.wh01s17.launcher.feature.widgets.native.clock
import kotlinx.coroutines.delay

/**
 * TIDAL hack: `sniff tidal`. La pista, el artista y (en grande) el álbum como salida, el estado,
 * el avance en ASCII, en grande el espectro con uno de los visualizadores de [HackViz] (tocarlo pasa
 * al siguiente y se recuerda) y las teclas `[prev]` `[play]`/`[pause]` `[next]`. Sin sesión, lo dice
 * con el cursor; sin acceso, `[grant]` lo pide.
 */
@Composable
internal fun HackMedia(
    sources: NativeWidgetSources,
    interactive: Boolean,
    modifier: Modifier,
) {
    val media = sources.media
    val state by remember(media) { media.session(TIDAL_PACKAGE) }.collectAsStateWithLifecycle(MediaState.Idle)
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var viz by remember { mutableStateOf(HackViz.fromId(prefs.getString(KEY_VIZ, null))) }
    HackFrame(modifier) { size ->
        if (size != WidgetSize.Small) HackPrompt(if (size >= WidgetSize.Large) "sniff tidal --viz ${viz.id}" else "sniff tidal")
        when (val current = state) {
            is MediaState.Session ->
                HackPlayer(current, sources, interactive, size, viz) {
                    viz = viz.next()
                    prefs.edit().putString(KEY_VIZ, viz.id).apply()
                }
            MediaState.Idle -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HackLine("stream", "--", WhTheme.colors.darkForeground)
                    HackCursor(WhTheme.typography.bodySmall)
                }
                WhText(
                    stringResource(R.string.native_hack_media_idle),
                    style = WhTheme.typography.bodySmall,
                    color = WhTheme.colors.darkForeground,
                )
                Spacer(Modifier.weight(1f))
                HackKeys { HackKey("open", stringResource(R.string.native_tidal_open), { open(context, tidal = true) }, interactive) }
            }
            MediaState.NoAccess -> {
                WhText(stringResource(R.string.native_hack_media_denied), style = WhTheme.typography.bodySmall, color = WhTheme.colors.red)
                Spacer(Modifier.weight(1f))
                HackKeys { HackKey("grant", stringResource(R.string.native_tidal_grant), { open(context, tidal = false) }, interactive) }
            }
        }
    }
}

@Composable
private fun ColumnScope.HackPlayer(
    state: MediaState.Session,
    sources: NativeWidgetSources,
    interactive: Boolean,
    size: WidgetSize,
    viz: HackViz,
    onNextViz: () -> Unit,
) {
    val media = sources.media
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state.playing) {
        while (state.playing) {
            now = SystemClock.elapsedRealtime()
            delay(TICK_MILLIS)
        }
    }
    val colors = WhTheme.colors
    val position = state.positionAt(now)
    HackLine("track", state.title.orEmpty(), colors.accent)
    if (size != WidgetSize.Small) HackLine("artist", state.artist.orEmpty())
    if (size >= WidgetSize.Large) HackLine("album", state.album.orEmpty())
    HackLine("state", if (state.playing) "[PLAYING]" else "[PAUSED]", if (state.playing) colors.accent else colors.yellow)
    val total = state.durationMillis
    HackMeter("", if (total > 0) position.toFloat() / total else 0f, "${clock(position)}/${clock(total)}", colors.accent)
    if (size >= WidgetSize.Large) {
        HackSpectrum(sources, viz, state.playing, interactive, onNextViz, Modifier.weight(1f).fillMaxWidth())
    } else {
        Spacer(Modifier.weight(1f))
    }
    HackKeys {
        HackKey("prev", stringResource(R.string.native_media_previous), { media.perform(state, MediaAction.Previous) }, interactive)
        HackKey(
            if (state.playing) "pause" else "play",
            stringResource(R.string.native_media_play_pause),
            { media.perform(state, MediaAction.PlayPause) },
            interactive,
            lit = state.playing,
        )
        HackKey("next", stringResource(R.string.native_media_next), { media.perform(state, MediaAction.Next) }, interactive)
    }
}

/** Abre TIDAL o la pantalla del acceso a notificaciones. */
private fun open(
    context: Context,
    tidal: Boolean,
) {
    val intent = if (tidal) context.packageManager.getLaunchIntentForPackage(TIDAL_PACKAGE) else MediaSource(context).accessSettings()
    try {
        intent?.let(context::startActivity)
    } catch (ignored: ActivityNotFoundException) {
        // Sin TIDAL o sin la pantalla de ajustes: el texto ya dice qué falta.
    }
}

private const val TICK_MILLIS = 500L

/** Las mismas preferencias que la forma del visualizador de nova; cada temática, su clave. */
private const val PREFS = "native-widgets"
private const val KEY_VIZ = "tidal.visualizer.hack"
