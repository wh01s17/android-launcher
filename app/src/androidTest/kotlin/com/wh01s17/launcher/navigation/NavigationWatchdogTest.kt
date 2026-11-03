package com.wh01s17.launcher.navigation

import android.app.ActivityManager
import android.app.UiAutomation
import android.content.ComponentName
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.wh01s17.launcher.LauncherApplication
import com.wh01s17.launcher.core.system.navigation.WhNavigationService
import com.wh01s17.launcher.core.system.oem.xiaomi.DeviceInfo
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Test obligatorio de plan.md §5.9: matar el servicio de navegación con los botones ocultos debe
 * dejar `force_fsg_nav_bar = 0` en menos de 1 s.
 *
 * Necesita un teléfono Xiaomi/Redmi/POCO con *Depuración USB (ajustes de seguridad)* activada (el
 * shell escribe ajustes protegidos). Pone este launcher como HOME, concede `WRITE_SECURE_SETTINGS`,
 * activa el servicio y el modo, mata el proceso `:nav` y mide. Al terminar deja el teléfono como
 * estaba: rol HOME, ajustes, servicios de accesibilidad y archivo del modo. Ojo: al acabar, Gradle
 * desinstala la app debug (y con ella su rol HOME y el permiso concedido).
 *
 *     ./gradlew :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class NavigationWatchdogTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val packageName = context.packageName
    private val service = ComponentName(context, WhNavigationService::class.java).flattenToString()
    private val watchdog get() = (context.applicationContext as LauncherApplication).container.navigationWatchdog
    private val modeFile get() = File(context.noBackupFilesDir, "navigation-mode.properties")

    private val saved = mutableMapOf<String, String>()
    private var savedHome: String = ""
    private var savedMode: String? = null

    @Before
    fun setUp() {
        assumeTrue("force_fsg_nav_bar sólo existe en HyperOS/MIUI", DeviceInfo.current().isXiaomi)
        for (key in SETTINGS) saved[key] = shell("settings get $key")
        savedHome = shell("cmd role get-role-holders android.app.role.HOME")
        savedMode = modeFile.takeIf { it.isFile }?.readText()

        shell("pm grant $packageName android.permission.WRITE_SECURE_SETTINGS")
        shell("cmd role add-role-holder android.app.role.HOME $packageName 0")
        instrumentation.runOnMainSync { watchdog.setModeEnabled(true) }
        shell("settings put secure enabled_accessibility_services $service")
        shell("settings put secure accessibility_enabled 1")
        assertTrue("the navigation service never hid the buttons", waitFor(SETUP_TIMEOUT_MILLIS) { forceFsg() == 1 } != null)
        // Como en el uso real: la vigilancia ya está enlazada cuando algo falla.
        assertTrue("the watchdog never linked to the navigation process", waitFor(SETUP_TIMEOUT_MILLIS) { watchdog.linked } != null)
    }

    @After
    fun tearDown() {
        if (saved.isEmpty()) return
        instrumentation.runOnMainSync { watchdog.setModeEnabled(false) }
        if (savedHome.isNotBlank()) shell("cmd role add-role-holder android.app.role.HOME $savedHome 0")
        // El rol primero: HyperOS reescribe force_fsg_nav_bar al cambiar de launcher.
        SystemClock.sleep(ROLE_SETTLE_MILLIS)
        for ((key, value) in saved) {
            if (value == "null" || value.isEmpty()) shell("settings delete $key") else shell("settings put $key $value")
        }
        savedMode?.let { modeFile.writeText(it) } ?: modeFile.delete()
    }

    @Test
    fun killingTheNavigationProcessGivesTheButtonsBackWithinOneSecond() {
        val navigation =
            context
                .getSystemService(ActivityManager::class.java)
                .runningAppProcesses
                .first { it.processName == "$packageName:nav" }
        val killedAt = SystemClock.elapsedRealtime()
        Process.killProcess(navigation.pid)
        val restoredAt = waitFor(RESTORE_BUDGET_MILLIS * 2) { forceFsg() == 0 }
        val elapsed = restoredAt?.minus(killedAt)
        assertTrue(
            "buttons restored after $elapsed ms (budget $RESTORE_BUDGET_MILLIS ms)",
            elapsed != null && elapsed < RESTORE_BUDGET_MILLIS,
        )
    }

    private fun forceFsg(): Int? = runCatching { Settings.Global.getInt(context.contentResolver, "force_fsg_nav_bar") }.getOrNull()

    /** Momento (`elapsedRealtime`) en que [condition] se cumplió, o `null` si no llegó a tiempo. */
    private fun waitFor(
        timeoutMillis: Long,
        condition: () -> Boolean,
    ): Long? {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return SystemClock.elapsedRealtime()
            SystemClock.sleep(POLL_MILLIS)
        }
        return null
    }

    /** Ejecuta como `shell` (puede escribir ajustes protegidos en Xiaomi con el interruptor de seguridad). */
    private fun shell(command: String): String {
        // Sin esta bandera, mientras UiAutomation está conectado Android suspende los demás
        // servicios de accesibilidad, el nuestro incluido (visto en el Redmi).
        val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val descriptor = automation.executeShellCommand(command)
        return ParcelFileDescriptor
            .AutoCloseInputStream(descriptor)
            .bufferedReader()
            .use { it.readText() }
            .trim()
    }

    private companion object {
        val SETTINGS =
            listOf(
                "global force_fsg_nav_bar",
                "global hide_gesture_line",
                "secure enabled_accessibility_services",
                "secure accessibility_enabled",
            )
        const val RESTORE_BUDGET_MILLIS = 1_000L
        const val SETUP_TIMEOUT_MILLIS = 10_000L
        const val ROLE_SETTLE_MILLIS = 1_500L
        const val POLL_MILLIS = 5L
    }
}
