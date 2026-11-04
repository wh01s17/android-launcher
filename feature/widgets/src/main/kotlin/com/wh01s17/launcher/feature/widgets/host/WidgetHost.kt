package com.wh01s17.launcher.feature.widgets.host

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * El `AppWidgetHost` del launcher (plan.md §5.8 y §7 Hito 7): reserva y libera ids, enlaza widgets,
 * los configura y crea sus vistas ([WhWidgetHostView]). Escucha actualizaciones sólo mientras el
 * inicio está visible ([startListening]/[stopListening]).
 */
class WidgetHost(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val manager = AppWidgetManager.getInstance(appContext)
    private val host =
        object : AppWidgetHost(appContext, HOST_ID) {
            override fun onCreateView(
                context: Context,
                appWidgetId: Int,
                appWidget: AppWidgetProviderInfo?,
            ): AppWidgetHostView = WhWidgetHostView(context)
        }

    fun startListening() = host.startListening()

    fun stopListening() = host.stopListening()

    fun allocate(): Int = host.allocateAppWidgetId()

    fun delete(appWidgetId: Int) = host.deleteAppWidgetId(appWidgetId)

    /** `null` si la app del widget ya no está (desinstalada o perfil quitado). */
    fun info(appWidgetId: Int): AppWidgetProviderInfo? = manager.getAppWidgetInfo(appWidgetId)

    /** Vista nueva del widget; se puede crear más de una para el mismo id. */
    fun createView(
        context: Context,
        appWidgetId: Int,
    ): WhWidgetHostView = host.createView(context, appWidgetId, info(appWidgetId)) as WhWidgetHostView

    /** `true` si Android deja enlazarlo sin preguntar; si no, hay que pedirlo con [bindIntent]. */
    fun bindIfAllowed(
        appWidgetId: Int,
        info: AppWidgetProviderInfo,
    ): Boolean = manager.bindAppWidgetIdIfAllowed(appWidgetId, info.profile, info.provider, null)

    /** El diálogo del sistema «¿Permitir que wh01s17 cree widgets?». */
    fun bindIntent(
        appWidgetId: Int,
        info: AppWidgetProviderInfo,
    ): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)

    /** El widget pide configurarse antes de añadirlo (y no lo marca como opcional). */
    fun needsConfigure(appWidgetId: Int): Boolean {
        val info = info(appWidgetId) ?: return false
        if (info.configure == null) return false
        val optional =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL != 0
        return !optional
    }

    /** Abre la configuración del widget; el resultado llega a `onActivityResult` con [requestCode]. */
    fun startConfigure(
        activity: Activity,
        appWidgetId: Int,
        requestCode: Int,
    ) = host.startAppWidgetConfigureActivityForResult(activity, appWidgetId, 0, requestCode, null)

    /**
     * Libera los ids que ningún tile usa (tiles quitados, enlaces a medias), salvo [keep] (el que se
     * está añadiendo). Así un widget quitado deja de recibir actualizaciones.
     */
    fun reconcile(
        used: Set<Int>,
        keep: Set<Int> = emptySet(),
    ) {
        for (id in host.appWidgetIds) if (id !in used && id !in keep) host.deleteAppWidgetId(id)
    }

    private companion object {
        /** Id fijo del host: los widgets enlazados se conservan entre versiones (plan.md §8.3). */
        const val HOST_ID = 0x5717
    }
}
