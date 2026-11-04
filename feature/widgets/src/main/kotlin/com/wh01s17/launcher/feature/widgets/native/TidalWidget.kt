package com.wh01s17.launcher.feature.widgets.native

import android.content.ActivityNotFoundException
import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wh01s17.launcher.core.designsystem.components.WhIcon
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.palette.PlayerPalette
import com.wh01s17.launcher.core.designsystem.palette.TidalampPalettes
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.media.MediaAction
import com.wh01s17.launcher.core.system.media.MediaSource
import com.wh01s17.launcher.core.system.media.MediaState
import com.wh01s17.launcher.feature.widgets.R
import kotlinx.coroutines.delay

/** El paquete de la app de TIDAL. */
internal const val TIDAL_PACKAGE = "com.aspiro.tidal"

/**
 * Control de TIDAL con el tema *nova* de tidalamp: sin marcos, «▍ tidalamp» en acento, carátula
 * junto al tiempo grande, la canción en acento, la línea de avance con punto (tocar salta) y la
 * banda de transporte con ⏮ ⏯ ■ ⏭ sin cajas (el que suena, con su regla de acento). La paleta es
 * de tidalamp: *phosphor* (la de wh01s17) de fábrica; tocar el título pasa a la siguiente y se
 * recuerda. Necesita el acceso a notificaciones (opt-in); sin él, o con TIDAL cerrado, ofrece abrirlo.
 */
@Composable
internal fun TidalWidget(
    sources: NativeWidgetSources,
    interactive: Boolean,
    modifier: Modifier = Modifier,
) {
    val media = sources.media
    val state by remember(media) { media.session(TIDAL_PACKAGE) }.collectAsStateWithLifecycle(MediaState.Idle)
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var palette by remember { mutableStateOf(TidalampPalettes.byName(prefs.getString(KEY_PALETTE, null))) }
    var shape by remember { mutableStateOf(VisualizerShape.fromConfig(prefs.getString(KEY_SHAPE, null))) }
    Column(
        modifier.fillMaxSize().background(palette.background).padding(WhTheme.spacing.controlGap),
        verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap),
    ) {
        NovaTitle(
            stringResource(R.string.native_tidal_title),
            "${palette.name} · ${shape.configName}",
            palette,
            stringResource(R.string.native_tidal_palette),
            onClick =
                if (interactive) {
                    {
                        palette = TidalampPalettes.after(palette)
                        prefs.edit().putString(KEY_PALETTE, palette.name).apply()
                    }
                } else {
                    null
                },
        )
        when (val current = state) {
            is MediaState.Session ->
                Player(current, sources, interactive, palette, shape, {
                    shape = shape.next()
                    prefs.edit().putString(KEY_SHAPE, shape.configName).apply()
                }, Modifier.weight(1f))
            MediaState.Idle -> Waiting(R.string.native_tidal_idle, interactive, palette, Modifier.weight(1f), openTidal = true)
            MediaState.NoAccess -> Waiting(R.string.native_tidal_no_access, interactive, palette, Modifier.weight(1f), openTidal = false)
        }
    }
}

@Composable
private fun Player(
    state: MediaState.Session,
    sources: NativeWidgetSources,
    interactive: Boolean,
    palette: PlayerPalette,
    shape: VisualizerShape,
    onShape: () -> Unit,
    modifier: Modifier,
) {
    val media = sources.media
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state.playing) {
        while (state.playing) {
            now = SystemClock.elapsedRealtime()
            delay(TICK_MILLIS)
        }
    }
    val position = state.positionAt(now)
    val fraction = if (state.durationMillis > 0) position.toFloat() / state.durationMillis else 0f
    // Arriba, pegado al título; el espacio que sobra es del visualizador.
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.panelGap)) {
            state.art?.let { art ->
                // Tocar la carátula abre TIDAL (como tocar un tile de app).
                val context = LocalContext.current
                val openLabel = stringResource(R.string.native_tidal_open)
                Image(
                    art.asImageBitmap(),
                    contentDescription = state.album,
                    modifier =
                        Modifier
                            .weight(COVER_WEIGHT)
                            .aspectRatio(1f)
                            .clickable(enabled = interactive, onClickLabel = openLabel) { open(context, openTidal = true) },
                    contentScale = ContentScale.Crop,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap / 2)) {
                WhText(clock(position), style = WhTheme.typography.displayLarge, color = palette.accent, maxLines = 1)
                Details(state, palette)
            }
        }
        WhText(
            state.title.orEmpty(),
            style = WhTheme.typography.subtitle,
            color = palette.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val status = stringResource(if (state.playing) R.string.native_tidal_playing else R.string.native_tidal_paused)
        val total = if (state.durationMillis > 0) " · ${clock(state.durationMillis)}" else ""
        WhText("$status$total", style = WhTheme.typography.caption, color = palette.dim, maxLines = 1)
        Visualizer(sources, state.playing, interactive, palette, shape, onShape, Modifier.weight(1f))
        NovaProgress(
            fraction,
            palette,
            onSeek =
                if (interactive &&
                    state.durationMillis > 0
                ) {
                    ({ media.seek(state, (it * state.durationMillis).toLong()) })
                } else {
                    null
                },
        )
        Transport(state, media, interactive, palette)
    }
}

/**
 * El analizador de tidalamp: sin permiso, una tecla «visualizador» que lo pide; con él, el espectro
 * mientras suena (en pausa queda vacío, como en tidalamp). Tocarlo cambia de forma (`fine` de
 * fábrica, luego `bars`, `mirror`, `curve`), y la forma se recuerda como la paleta.
 */
@Composable
private fun Visualizer(
    sources: NativeWidgetSources,
    playing: Boolean,
    interactive: Boolean,
    palette: PlayerPalette,
    shape: VisualizerShape,
    onShape: () -> Unit,
    modifier: Modifier,
) {
    val permitted by sources.audioPermission.collectAsStateWithLifecycle()
    val description = stringResource(R.string.native_tidal_visualizer_shape, shape.configName)
    val tap =
        if (interactive && permitted) {
            Modifier.semantics { contentDescription = description }.clickable(
                remember { MutableInteractionSource() },
                null,
                onClick = onShape,
            )
        } else {
            Modifier
        }
    Box(modifier.fillMaxWidth().then(tap), contentAlignment = Alignment.Center) {
        when {
            !permitted -> {
                val label = stringResource(R.string.native_tidal_visualizer)
                NovaKey(label, sources.requestAudio, palette, interactive) { WhText(label, color = it) }
            }
            playing -> {
                val bands by remember(sources) { sources.spectrum.bands(SPECTRUM_BANDS) }
                    .collectAsStateWithLifecycle(FloatArray(SPECTRUM_BANDS))
                val peaks = remember { FloatArray(SPECTRUM_BANDS) }
                fallPeaks(peaks, bands)
                Spectrum(bands, peaks, shape, palette, Modifier.fillMaxSize())
            }
        }
    }
}

/** Artista y álbum en el color apagado, uno por línea, como la columna de lectura de tidalamp. */
@Composable
private fun Details(
    state: MediaState.Session,
    palette: PlayerPalette,
) {
    for (line in listOfNotNull(state.artist, state.album)) {
        WhText(line, style = WhTheme.typography.bodySmall, color = palette.dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Transport(
    state: MediaState.Session,
    media: MediaSource,
    interactive: Boolean,
    palette: PlayerPalette,
) {
    NovaBand(palette) {
        NovaKey(stringResource(R.string.native_media_previous), { media.perform(state, MediaAction.Previous) }, palette, interactive) {
            WhIcon(WhIcons.SkipPrevious, tint = it)
        }
        NovaKey(
            stringResource(R.string.native_media_play_pause),
            { media.perform(state, MediaAction.PlayPause) },
            palette,
            interactive,
            lit = state.playing,
        ) { WhIcon(if (state.playing) WhIcons.Pause else WhIcons.Play, tint = it) }
        NovaKey(
            stringResource(R.string.native_media_stop),
            { media.perform(state, MediaAction.Stop) },
            palette,
            interactive,
        ) { NovaStop(it) }
        NovaKey(stringResource(R.string.native_media_next), { media.perform(state, MediaAction.Next) }, palette, interactive) {
            WhIcon(WhIcons.SkipNext, tint = it)
        }
    }
}

/** Sin sesión: explica qué falta y ofrece abrir TIDAL o los ajustes de acceso a notificaciones. */
@Composable
private fun Waiting(
    message: Int,
    interactive: Boolean,
    palette: PlayerPalette,
    modifier: Modifier,
    openTidal: Boolean,
) {
    val context = LocalContext.current
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WhText(stringResource(message), style = WhTheme.typography.bodySmall, color = palette.dim)
        val label = stringResource(if (openTidal) R.string.native_tidal_open else R.string.native_tidal_grant)
        NovaKey(label, { open(context, openTidal) }, palette, interactive, lit = true) { WhText(label, color = it) }
    }
}

/** Abre TIDAL o la pantalla del acceso a notificaciones. */
private fun open(
    context: Context,
    openTidal: Boolean,
) {
    val intent = if (openTidal) context.packageManager.getLaunchIntentForPackage(TIDAL_PACKAGE) else MediaSource(context).accessSettings()
    try {
        intent?.let(context::startActivity)
    } catch (ignored: ActivityNotFoundException) {
        // Sin TIDAL o sin la pantalla de ajustes: el texto ya explica qué falta.
    }
}

/** `m:ss` (o `h:mm:ss`), como el reloj de tidalamp. */
internal fun clock(millis: Long): String {
    val seconds = millis / MILLIS_PER_SECOND
    val h = seconds / SECONDS_PER_HOUR
    val m = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val s = seconds % SECONDS_PER_MINUTE
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private const val TICK_MILLIS = 500L
private const val COVER_WEIGHT = 0.4f
private const val SPECTRUM_BANDS = 48
private const val PREFS = "native-widgets"
private const val KEY_PALETTE = "tidal.palette"
private const val KEY_SHAPE = "tidal.visualizer"
private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3600L
