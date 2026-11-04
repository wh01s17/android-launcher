package com.wh01s17.launcher.feature.widgets.native

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.media.AudioSpectrum
import com.wh01s17.launcher.core.system.media.MediaSource
import com.wh01s17.launcher.core.system.status.ClockSource
import com.wh01s17.launcher.core.system.timer.MetronomeRepository
import com.wh01s17.launcher.core.system.timer.PomodoroRepository
import com.wh01s17.launcher.feature.widgets.R
import kotlinx.coroutines.flow.StateFlow

/** Los widgets propios de wh01s17 y su nombre en `workspaces.json` (`{"native": "pomodoro"}`). */
enum class NativeWidget(
    val id: String,
    @StringRes val label: Int,
) {
    Tidal("tidal", R.string.native_tidal),
    Pomodoro("pomodoro", R.string.native_pomodoro),
    Metronome("metronome", R.string.native_metronome),
    Clock("clock", R.string.native_clock),
    Usage("usage", R.string.native_usage),
    ;

    companion object {
        fun fromId(id: String): NativeWidget? = entries.firstOrNull { it.id == id }
    }
}

/** Lo que necesitan los widgets propios; los crea la app una vez (estado compartido con la barra). */
class NativeWidgetSources(
    val media: MediaSource,
    val pomodoro: PomodoroRepository,
    val metronome: MetronomeRepository,
    val clock: ClockSource,
    val spectrum: AudioSpectrum,
    /** Si el visualizador tiene el permiso de micrófono; la app lo actualiza al volver del diálogo. */
    val audioPermission: StateFlow<Boolean>,
    /** Pide el permiso de micrófono (sólo al tocar «visualizador», plan.md R8). */
    val requestAudio: () -> Unit,
)

/** Pinta el widget propio [id]; uno desconocido (de una versión futura) se ve como su nombre. */
@Composable
fun NativeWidgetView(
    id: String,
    sources: NativeWidgetSources,
    interactive: Boolean,
    modifier: Modifier = Modifier,
) {
    when (NativeWidget.fromId(id)) {
        NativeWidget.Tidal -> TidalWidget(sources, interactive, modifier)
        NativeWidget.Pomodoro -> PomodoroWidget(sources.pomodoro, interactive, modifier)
        NativeWidget.Metronome -> MetronomeWidget(sources.metronome, interactive, modifier)
        NativeWidget.Clock -> ClockWidget(sources.clock, modifier)
        NativeWidget.Usage -> UsageWidget(modifier)
        null ->
            Box(modifier, contentAlignment = Alignment.Center) {
                WhText(stringResource(R.string.native_unknown, id), color = WhTheme.colors.darkForeground)
            }
    }
}
