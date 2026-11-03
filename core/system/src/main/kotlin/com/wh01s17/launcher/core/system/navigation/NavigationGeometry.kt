package com.wh01s17.launcher.core.system.navigation

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityWindowInfo
import kotlin.math.roundToInt

/**
 * Medidas de la capa 3 para el contexto del servicio: tamaño real de la pantalla, alto de la barra
 * de estado, teclado abierto, pantalla de bloqueo y umbrales de los gestos (en dp, pasados a px).
 */
internal class NavigationGeometry(
    private val context: Context,
) {
    private val density get() = context.resources.displayMetrics.density

    fun configFor(edge: Edge): EdgeGestureConfig {
        val trigger = if (edge == Edge.Bottom) HOME_TRIGGER_DP else BACK_TRIGGER_DP
        return EdgeGestureConfig(triggerDistancePx = trigger * density, stillSlopPx = STILL_SLOP_DP * density)
    }

    /** [windows] son las ventanas visibles (`AccessibilityService.getWindows`), para ver el teclado. */
    fun layoutInput(windows: List<AccessibilityWindowInfo>): EdgeLayoutInput {
        val (width, height, statusBar) = screenGeometry()
        return EdgeLayoutInput(
            screenWidth = width,
            screenHeight = height,
            sideThicknessPx = (SIDE_THICKNESS_DP * density).roundToInt(),
            bottomThicknessPx = (BOTTOM_THICKNESS_DP * density).roundToInt(),
            topExclusionPx = statusBar,
            imeTop = imeTop(windows),
            suspended = context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true,
        )
    }

    /** Ancho, alto y alto de la barra de estado, en píxeles de la pantalla real. */
    private fun screenGeometry(): Triple<Int, Int, Int> {
        val windowManager = context.getSystemService(WindowManager::class.java)
        val fallbackStatus = (FALLBACK_STATUS_BAR_DP * density).roundToInt()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            val insets = windowManager.currentWindowMetrics.windowInsets
            val status = insets.getInsets(WindowInsets.Type.statusBars() or WindowInsets.Type.displayCutout()).top
            Triple(bounds.width(), bounds.height(), status.takeIf { it > 0 } ?: fallbackStatus)
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            Triple(metrics.widthPixels, metrics.heightPixels, fallbackStatus)
        }
    }

    private fun imeTop(windows: List<AccessibilityWindowInfo>): Int? =
        windows
            .firstOrNull { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
            ?.let { window -> Rect().also(window::getBoundsInScreen).top }

    private companion object {
        // Valores de partida; el Hito 6 los hace configurables (plan.md §7).
        const val SIDE_THICKNESS_DP = 14f
        const val BOTTOM_THICKNESS_DP = 16f
        const val BACK_TRIGGER_DP = 32f
        const val HOME_TRIGGER_DP = 40f
        const val STILL_SLOP_DP = 8f
        const val FALLBACK_STATUS_BAR_DP = 32f
    }
}
