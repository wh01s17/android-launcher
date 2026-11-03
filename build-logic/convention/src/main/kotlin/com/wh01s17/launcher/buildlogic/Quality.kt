package com.wh01s17.launcher.buildlogic

import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/**
 * ktlint, detekt y Kover para cualquier módulo Kotlin (plan.md §1 R4 y R7).
 *
 * detekt 2.0 está en alpha: detekt 1.23 no puede ejecutarse con el JDK 25 de
 * Android Studio (plan.md §11.1).
 */
internal fun Project.configureQuality() {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    pluginManager.apply("dev.detekt")
    pluginManager.apply("org.jetbrains.kotlinx.kover")

    extensions.configure<KtlintExtension> {
        // Código generado (p. ej. WhTokens): se valida compilándolo, no formateándolo.
        filter { exclude("**/generated/**") }
    }

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    }
}
