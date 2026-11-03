import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.wh01s17.launcher.buildlogic.moduleNamespace
import com.wh01s17.launcher.buildlogic.tokens.GenerateTokensTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/**
 * Registra `generateTokens` y lo conecta a cada variante como fuente generada
 * (plan.md §4.1). Se aplica sólo a `:core:designsystem`, después de
 * `wh01s17.android.library`.
 */
class DesignTokensConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            val generate =
                tasks.register<GenerateTokensTask>("generateTokens") {
                    group = "build"
                    description = "Genera WhTokens y los colores de Android desde theme/wh01s17."
                    themeDir.set(rootProject.layout.projectDirectory.dir("theme/wh01s17"))
                    packageName.set("${moduleNamespace()}.tokens")
                    kotlinOutputDir.set(layout.buildDirectory.dir("generated/source/wh01s17Tokens/kotlin"))
                    resOutputDir.set(layout.buildDirectory.dir("generated/source/wh01s17Tokens/res"))
                }

            extensions.configure<LibraryAndroidComponentsExtension> {
                onVariants { variant ->
                    variant.sources.kotlin?.addGeneratedSourceDirectory(generate, GenerateTokensTask::kotlinOutputDir)
                    variant.sources.res?.addGeneratedSourceDirectory(generate, GenerateTokensTask::resOutputDir)
                }
            }
        }
}
