import com.android.build.api.dsl.TestExtension
import com.wh01s17.launcher.buildlogic.ProjectConfig
import com.wh01s17.launcher.buildlogic.configureQuality
import com.wh01s17.launcher.buildlogic.moduleNamespace
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Módulo de pruebas sobre otro (`com.android.test`): el de Macrobenchmark y Baseline Profile. */
class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.test")

            extensions.configure<TestExtension> {
                namespace = moduleNamespace()
                compileSdk = ProjectConfig.COMPILE_SDK
                defaultConfig {
                    minSdk = ProjectConfig.MIN_SDK
                    targetSdk = ProjectConfig.TARGET_SDK
                }
                compileOptions {
                    sourceCompatibility = ProjectConfig.JAVA_VERSION
                    targetCompatibility = ProjectConfig.JAVA_VERSION
                }
            }
            configureQuality()
        }
}
