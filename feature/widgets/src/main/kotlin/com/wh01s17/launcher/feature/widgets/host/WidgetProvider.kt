package com.wh01s17.launcher.feature.widgets.host

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.PackageManager

/** Un widget que se puede añadir: lo que muestra el selector. */
data class WidgetProvider(
    val info: AppWidgetProviderInfo,
    val label: String,
    val appLabel: String,
    /** Tamaño mínimo que pide el widget, en dp. */
    val minWidthDp: Int,
    val minHeightDp: Int,
)

/** Los widgets instalados en todos los perfiles, por app y nombre. Bloqueante: fuera del hilo principal. */
fun installedWidgets(context: Context): List<WidgetProvider> {
    val pm = context.packageManager
    val density = context.resources.displayMetrics.density
    return AppWidgetManager
        .getInstance(context)
        .installedProviders
        .map { info ->
            WidgetProvider(
                info = info,
                label = info.loadLabel(pm),
                appLabel = appLabel(pm, info.provider.packageName),
                minWidthDp = (info.minWidth / density).toInt(),
                minHeightDp = (info.minHeight / density).toInt(),
            )
        }.sortedWith(
            compareBy(String.CASE_INSENSITIVE_ORDER, WidgetProvider::appLabel).thenBy(String.CASE_INSENSITIVE_ORDER, WidgetProvider::label),
        )
}

private fun appLabel(
    pm: PackageManager,
    packageName: String,
): String =
    try {
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (ignored: PackageManager.NameNotFoundException) {
        packageName
    }
