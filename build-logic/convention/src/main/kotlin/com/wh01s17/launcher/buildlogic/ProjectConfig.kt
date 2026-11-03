package com.wh01s17.launcher.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** Valores comunes a todos los módulos (plan.md §2.3 y §5.1). */
object ProjectConfig {
    const val NAMESPACE_ROOT = "com.wh01s17.launcher"
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 37
    const val MIN_SDK = 29
    val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_17
}

/** `:core:tiling` → `com.wh01s17.launcher.core.tiling`. */
fun Project.moduleNamespace(): String =
    ProjectConfig.NAMESPACE_ROOT + path.replace(':', '.').replace('-', '_').trimEnd('.')

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
