package com.wh01s17.launcher.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Genera el Baseline Profile (`./gradlew :app:generateReleaseBaselineProfile`): el arranque, deslizar
 * escritorios y abrir el menú con una búsqueda, lo que más se usa. Ver docs/performance.md.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() =
        rule.collect(packageName = TARGET_PACKAGE, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()
            swipeWorkspaces()
            openAndCloseMenu(query = "set")
            openAndCloseMenu()
        }
}
