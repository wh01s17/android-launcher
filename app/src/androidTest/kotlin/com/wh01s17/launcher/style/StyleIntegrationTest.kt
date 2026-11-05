package com.wh01s17.launcher.style

import android.content.Intent
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.wh01s17.launcher.LauncherApplication
import com.wh01s17.launcher.MainActivity
import com.wh01s17.launcher.ScreensaverActivity
import com.wh01s17.launcher.core.config.AndroidConfigSource
import com.wh01s17.launcher.core.config.EditResult
import com.wh01s17.launcher.core.designsystem.screensaver.WhSpinView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Runs on the target device, restoring the original config and checking workspace bytes. */
@RunWith(AndroidJUnit4::class)
class StyleIntegrationTest {
    @Test
    fun idleWakeAndConfigRoundTripPreserveDesktop() =
        runBlocking {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            val container = (context.applicationContext as LauncherApplication).container
            val source = AndroidConfigSource(context)
            val original = source.readUserConfig()
            val desktop = File(context.filesDir, "state/workspaces.json")
            container.desk.state.first { it != null }
            container.desk.flush()
            val desktopBefore = desktop.takeIf { it.exists() }?.readBytes()
            try {
                container.configRepository.reload()
                val export = container.configRepository.exportDocument()
                assertEquals(EditResult.Saved, container.configRepository.importDocument(export))
                assertTrue(container.configRepository.importDocument("broken") is EditResult.Refused)
                assertEquals(export, container.configRepository.exportDocument())
                val activity = launchHome(context)
                val patch = Json.parseToJsonElement("""{"idle":{"screensaver":1},"appearance":{"fontScale":1.5}}""") as JsonObject
                assertEquals(EditResult.Saved, container.configRepository.editValues(patch))
                assertEquals(
                    1.5f,
                    container.configRepository.state.value.config!!
                        .appearance.fontScale,
                )
                SystemClock.sleep(1800)
                instrumentation.runOnMainSync { assertTrue(containsSpin(activity.window.decorView)) }
                instrumentation.runOnMainSync {
                    val now = SystemClock.uptimeMillis()
                    val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 100f, 100f, 0)
                    val up = MotionEvent.obtain(now, now + 10, MotionEvent.ACTION_UP, 100f, 100f, 0)
                    activity.dispatchTouchEvent(down)
                    activity.dispatchTouchEvent(up)
                    down.recycle()
                    up.recycle()
                }
                SystemClock.sleep(200)
                instrumentation.runOnMainSync { assertFalse(containsSpin(activity.window.decorView)) }
                container.desk.flush()
                assertTrue(desktopBefore.contentEquals(desktop.takeIf { it.exists() }?.readBytes()))
                val preview =
                    instrumentation.startActivitySync(
                        Intent(context, ScreensaverActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                instrumentation.runOnMainSync {
                    assertTrue(containsSpin(preview.window.decorView))
                    preview.finish()
                }
            } finally {
                if (original != null) {
                    source.writeUserConfig(original)
                } else {
                    File(context.filesDir, "config/launcher.json").delete()
                }
                container.configRepository.reload()
            }
        }

    /**
     * `MainActivity` es `singleTask` y, como HOME, casi siempre ya existe: Android reutiliza esa
     * instancia (`onNewIntent`) y `startActivitySync` esperaría para siempre una nueva. Se lanza y se
     * toma la instancia reanudada; no se cierra al terminar, porque es el launcher del usuario.
     */
    private fun launchHome(context: android.content.Context): MainActivity {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        repeat(LAUNCH_ATTEMPTS) {
            instrumentation.waitForIdleSync()
            var home: MainActivity? = null
            instrumentation.runOnMainSync {
                home =
                    ActivityLifecycleMonitorRegistry
                        .getInstance()
                        .getActivitiesInStage(Stage.RESUMED)
                        .filterIsInstance<MainActivity>()
                        .firstOrNull()
            }
            home?.let { return it }
            SystemClock.sleep(LAUNCH_POLL_MILLIS)
        }
        error("MainActivity never resumed")
    }

    private companion object {
        const val LAUNCH_ATTEMPTS = 50
        const val LAUNCH_POLL_MILLIS = 100L
    }

    private fun containsSpin(view: View): Boolean =
        view is WhSpinView ||
            (view is ViewGroup && (0 until view.childCount).any { containsSpin(view.getChildAt(it)) })
}
