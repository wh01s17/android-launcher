import com.android.build.api.dsl.LibraryExtension
import com.wh01s17.launcher.buildlogic.ProjectConfig
import com.wh01s17.launcher.buildlogic.configureQuality
import com.wh01s17.launcher.buildlogic.moduleNamespace
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                namespace = moduleNamespace()
                compileSdk = ProjectConfig.COMPILE_SDK
                defaultConfig {
                    minSdk = ProjectConfig.MIN_SDK
                }
                compileOptions {
                    sourceCompatibility = ProjectConfig.JAVA_VERSION
                    targetCompatibility = ProjectConfig.JAVA_VERSION
                }
            }
            configureQuality()
        }
}
