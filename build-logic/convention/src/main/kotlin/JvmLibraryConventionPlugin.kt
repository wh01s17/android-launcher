import com.wh01s17.launcher.buildlogic.ProjectConfig
import com.wh01s17.launcher.buildlogic.configureQuality
import com.wh01s17.launcher.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** Módulos Kotlin puros, sin Android (plan.md §1 R7: `:core:tiling`, `:core:fuzzy`). */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = ProjectConfig.JAVA_VERSION
                targetCompatibility = ProjectConfig.JAVA_VERSION
            }
            extensions.configure<KotlinJvmProjectExtension> {
                compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
            }

            dependencies {
                add("testImplementation", platform(libs.findLibrary("junit-bom").get()))
                add("testImplementation", libs.findLibrary("junit-jupiter").get())
                add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
            }
            tasks.withType<Test>().configureEach { useJUnitPlatform() }

            configureQuality()
        }
}
