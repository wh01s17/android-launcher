package com.wh01s17.launcher.buildlogic.tokens

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Genera `WhTokens.kt` y `wh_tokens.xml` desde `theme/wh01s17` (plan.md §4.1). */
@CacheableTask
abstract class GenerateTokensTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val themeDir: DirectoryProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:OutputDirectory
    abstract val kotlinOutputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val resOutputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val generated = TokenGenerator.generate(ThemeSource.load(themeDir.get().asFile), packageName.get())

        val kotlinDir = kotlinOutputDir.get().asFile.also { it.deleteRecursively() }
        val packageDir = kotlinDir.resolve(packageName.get().replace('.', '/')).also { it.mkdirs() }
        packageDir.resolve("WhTokens.kt").writeText(generated.kotlin)

        val valuesDir = resOutputDir.get().asFile.also { it.deleteRecursively() }.resolve("values").also { it.mkdirs() }
        valuesDir.resolve("wh_tokens.xml").writeText(generated.colorResources)
    }
}
