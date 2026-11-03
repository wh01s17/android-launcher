package com.wh01s17.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import android.graphics.Color as AndroidColor

/**
 * Pantalla de inicio (rol HOME). Por ahora sólo dibuja el fondo wh01s17;
 * los workspaces llegan en el Hito 4 (plan.md §7).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // El tema wh01s17 es siempre oscuro: iconos claros en las barras del
        // sistema aunque el teléfono esté en modo claro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        // Un launcher no se cierra con "atrás".
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = Unit
            },
        )
        setContent {
            WhTheme {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(WhTheme.colors.background),
                )
            }
        }
    }
}
