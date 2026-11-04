package com.wh01s17.launcher.widgets

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wh01s17.launcher.feature.widgets.host.WidgetHost
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
    onAdded: (Int) -> Unit,
) {
    private val flow = WidgetFlow(activity, host, onAdded)

    private var picking by mutableStateOf(false)

    val renderer =
        WidgetRenderer { appWidgetId, interactive, onLongPress, modifier ->
            WidgetView(host, appWidgetId, interactive, onLongPress, modifier)
        }

    init {
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) = host.startListening()

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

    fun onConfigureResult(
        requestCode: Int,
        resultCode: Int,
    ) = flow.onConfigureResult(requestCode, resultCode)

    @Composable
    fun Picker() {
        if (!picking) return
        WidgetPicker(
            onPick = { provider ->
                picking = false
                flow.start(provider.info)
            },
            onDismiss = { picking = false },
        )
    }
}
