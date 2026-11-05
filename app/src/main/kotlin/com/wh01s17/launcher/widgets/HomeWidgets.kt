package com.wh01s17.launcher.widgets

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wh01s17.launcher.feature.widgets.host.WidgetHost
import com.wh01s17.launcher.feature.widgets.native.NativeWidgetSources
import com.wh01s17.launcher.feature.widgets.native.NativeWidgetView
import com.wh01s17.launcher.feature.widgets.ui.WidgetPicker
import com.wh01s17.launcher.feature.widgets.ui.WidgetView
import com.wh01s17.launcher.feature.workspaces.model.Desk
import com.wh01s17.launcher.feature.workspaces.model.TileContent
import com.wh01s17.launcher.feature.workspaces.ui.WidgetRenderer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Los widgets de la pantalla de inicio (plan.md §7 Hito 7): el selector, el flujo para añadirlos,
 * cómo se pintan dentro de los tiles y la limpieza de ids que ya no usa ningún tile. El host sólo
 * escucha actualizaciones mientras el inicio se ve. Se crea con la actividad (antes de `onStart`).
 */
internal class HomeWidgets(
    private val activity: ComponentActivity,
    private val host: WidgetHost,
    desk: StateFlow<Desk?>,
    private val sources: NativeWidgetSources,
    private val onAudioPermission: (Boolean) -> Unit,
    setRequestAudio: (() -> Unit) -> Unit,
    onAdded: (Int) -> Unit,
    private val onAddNative: (String) -> Unit,
) {
    private val flow = WidgetFlow(activity, host, onAdded)

    private var picking by mutableStateOf(false)

    val renderer =
        object : WidgetRenderer {
            @Composable
            override fun Content(
                appWidgetId: Int,
                interactive: Boolean,
                onLongPress: () -> Unit,
                modifier: Modifier,
            ) = WidgetView(host, appWidgetId, interactive, onLongPress, modifier)

            @Composable
            override fun Native(
                id: String,
                interactive: Boolean,
                modifier: Modifier,
            ) = NativeWidgetView(id, sources, interactive, modifier)
        }

    /** Permiso de micrófono para el visualizador; se pide sólo desde el widget (plan.md R8). */
    private val audioPermission =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> onAudioPermission(granted) }

    init {
        setRequestAudio { audioPermission.launch(Manifest.permission.RECORD_AUDIO) }
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    host.startListening()
                    onAudioPermission(sources.spectrum.permitted())
                }

                override fun onStop(owner: LifecycleOwner) = host.stopListening()
            },
        )
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                desk
                    .filterNotNull()
                    .map { d ->
                        d.contents.values
                            .filterIsInstance<TileContent.Widget>()
                            .mapTo(mutableSetOf()) { it.appWidgetId }
                    }.distinctUntilChanged()
                    .collect { used -> host.reconcile(used, keep = setOfNotNull(flow.pending)) }
            }
        }
    }

    fun openPicker() {
        picking = true
    }

    /** Escape del teclado físico: cierra el selector si está abierto (`true` si lo cerró). */
    fun closePicker(): Boolean {
        if (!picking) return false
        picking = false
        return true
    }

    val pickerOpen: Boolean get() = picking

    fun onConfigureResult(
        requestCode: Int,
        resultCode: Int,
    ) = flow.onConfigureResult(requestCode, resultCode)

    @Composable
    fun Picker() {
        if (!picking) return
        // Atrás (gesto, botón o Escape) cierra el selector, como cualquier otra ventana del inicio.
        BackHandler { picking = false }
        WidgetPicker(
            onPick = { provider ->
                picking = false
                flow.start(provider.info)
            },
            onPickNative = { widget ->
                picking = false
                onAddNative(widget.id)
            },
            onDismiss = { picking = false },
        )
    }
}
