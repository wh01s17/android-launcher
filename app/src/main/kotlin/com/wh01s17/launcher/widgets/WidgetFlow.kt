package com.wh01s17.launcher.widgets

import android.app.Activity
import android.appwidget.AppWidgetProviderInfo
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.wh01s17.launcher.feature.widgets.R
import com.wh01s17.launcher.feature.widgets.host.WidgetHost

/**
 * Añadir un widget (plan.md §5.8 y §7 Hito 7): reservar un id, enlazarlo (con el diálogo del sistema
 * si Android no lo permite directamente), configurarlo si el widget lo pide y entonces crear el
 * tile con [onAdded]. Si algo se cancela, el id se libera. Se crea en `onCreate` (registra un
 * resultado de actividad); el de configuración llega por [onConfigureResult].
 */
internal class WidgetFlow(
    private val activity: ComponentActivity,
    private val host: WidgetHost,
    private val onAdded: (Int) -> Unit,
) {
    /** El id que se está añadiendo: la limpieza de ids huérfanos no debe borrarlo. */
    var pending: Int? = null
        private set

    private val bind =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val id = pending ?: return@registerForActivityResult
            if (result.resultCode == Activity.RESULT_OK) {
                configureOrAdd(id)
            } else {
                Toast.makeText(activity, R.string.widget_bind_denied, Toast.LENGTH_LONG).show()
                cancel(id)
            }
        }

    fun start(info: AppWidgetProviderInfo) {
        pending?.let(::cancel)
        val id = host.allocate()
        pending = id
        if (host.bindIfAllowed(id, info)) configureOrAdd(id) else bind.launch(host.bindIntent(id, info))
    }

    /** Desde `onActivityResult`; `true` si era la configuración de un widget. */
    fun onConfigureResult(
        requestCode: Int,
        resultCode: Int,
    ): Boolean {
        if (requestCode != REQUEST_CONFIGURE) return false
        val id = pending ?: return true
        if (resultCode == Activity.RESULT_OK) add(id) else cancel(id)
        return true
    }

    private fun configureOrAdd(id: Int) {
        if (host.needsConfigure(id)) host.startConfigure(activity, id, REQUEST_CONFIGURE) else add(id)
    }

    private fun add(id: Int) {
        pending = null
        onAdded(id)
    }

    private fun cancel(id: Int) {
        pending = null
        host.delete(id)
    }

    private companion object {
        const val REQUEST_CONFIGURE = 0x5717
    }
}
