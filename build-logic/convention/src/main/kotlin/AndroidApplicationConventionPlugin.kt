import com.android.build.api.dsl.ApplicationExtension
import com.wh01s17.launcher.buildlogic.ProjectConfig
import com.wh01s17.launcher.buildlogic.configureQuality
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                namespace = ProjectConfig.NAMESPACE_ROOT
                compileSdk = ProjectConfig.COMPILE_SDK
                defaultConfig {
                    applicationId = ProjectConfig.NAMESPACE_ROOT
                    minSdk = ProjectConfig.MIN_SDK
                    targetSdk = ProjectConfig.TARGET_SDK
                }
                compileOptions {
                    sourceCompatibility = ProjectConfig.JAVA_VERSION
                    targetCompatibility = ProjectConfig.JAVA_VERSION
                }
                buildTypes {
                    getByName("debug") {
                        applicationIdSuffix = ".debug"
                    }
                    // Para el uso diario en el teléfono: no depurable (ART compila el código; en debug
                    // casi todo va por el intérprete) y perfilable para simpleperf. Mismo id y firma
                    // que debug, así que se instala encima conservando escritorios y ajustes.
                    // Sin R8 hasta el Hito 11 (plan.md §7), que añade reglas y Baseline Profile.
                    create("fast") {
                        initWith(getByName("release"))
                        applicationIdSuffix = ".debug"
                        signingConfig = signingConfigs.getByName("debug")
                        matchingFallbacks += "release"
                        isDebuggable = false
                        isProfileable = true
                    }
                }
            }
            configureQuality()
        }
}
