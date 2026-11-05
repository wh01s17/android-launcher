package com.wh01s17.launcher.ui

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wh01s17.launcher.core.config.ConfigRepository
import com.wh01s17.launcher.core.designsystem.screensaver.WhSpinView
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Sólo cuenta la inactividad con HOME reanudado; salir de HOME siempre quita la capa. */
internal class IdleScreensaver(
    private val activity: ComponentActivity,
    private val config: ConfigRepository,
) {
    private val interaction = MutableStateFlow(SystemClock.elapsedRealtime())
    val visible = MutableStateFlow(false)

    fun touch() {
        visible.value = false
        interaction.value = SystemClock.elapsedRealtime()
    }

    fun onKey(event: android.view.KeyEvent): Boolean {
        val consume = visible.value
        if (!consume || event.action == android.view.KeyEvent.ACTION_UP) touch()
        return consume
    }

    fun immersive(enabled: Boolean) {
        val insets = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (enabled) insets.hide(WindowInsetsCompat.Type.systemBars()) else insets.show(WindowInsetsCompat.Type.systemBars())
    }

    fun start() {
        activity.window.decorView.viewTreeObserver
            .addOnWindowFocusChangeListener { touch() }
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                touch()
                try {
                    combine(config.state, interaction) { state, last -> (state.config?.idle?.screensaver ?: 0) to last }
                        .collectLatest { (seconds, last) ->
                            visible.value = false
                            if (seconds > 0) {
                                delay((seconds * MILLIS_PER_SECOND - (SystemClock.elapsedRealtime() - last)).coerceAtLeast(0))
                                visible.value = activity.hasWindowFocus()
                            }
                        }
                } finally {
                    visible.value = false
                }
            }
        }
    }
}

@Composable
internal fun IdleOverlay(idle: IdleScreensaver) {
    val visible by idle.visible.collectAsStateWithLifecycle()
    if (visible) {
        DisposableEffect(idle) {
            idle.immersive(true)
            onDispose { idle.immersive(false) }
        }
        val accent = WhTheme.colors.accent
        AndroidView(factory = { WhSpinView(it, accent) }, modifier = Modifier.fillMaxSize())
    }
}

private const val MILLIS_PER_SECOND = 1000L
