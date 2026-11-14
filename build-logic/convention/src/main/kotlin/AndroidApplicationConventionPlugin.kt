import com.android.build.api.dsl.ApplicationExtension
import com.wh01s17.launcher.buildlogic.ProjectConfig
import com.wh01s17.launcher.buildlogic.configureQuality
import com.wh01s17.launcher.buildlogic.release.AppVersion
import com.wh01s17.launcher.buildlogic.release.ReleaseSigning
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.application")
            val version = AppVersion.read(rootProject.file("version.properties"))
            val signing =
                ReleaseSigning.resolve(
                    env = { providers.environmentVariable(it).orNull },
                    propertiesFile = rootProject.file("keystore.properties"),
                )

            extensions.configure<ApplicationExtension> {
                namespace = ProjectConfig.NAMESPACE_ROOT
                compileSdk = ProjectConfig.COMPILE_SDK
                defaultConfig {
                    applicationId = ProjectConfig.NAMESPACE_ROOT
                    minSdk = ProjectConfig.MIN_SDK
                    targetSdk = ProjectConfig.TARGET_SDK
                    versionCode = version.code
                    versionName = version.name
                }
                compileOptions {
                    sourceCompatibility = ProjectConfig.JAVA_VERSION
                    targetCompatibility = ProjectConfig.JAVA_VERSION
                }
                if (signing != null) {
                    signingConfigs.create("release") {
                        storeFile = signing.storeFile
                        storePassword = signing.storePassword
                        keyAlias = signing.keyAlias
                        keyPassword = signing.keyPassword
                    }
                }
                configureBuildTypes(this, hasReleaseKey = signing != null)
            }
            configureQuality()
        }

    private fun Project.configureBuildTypes(
        android: ApplicationExtension,
        hasReleaseKey: Boolean,
    ) = with(android) {
        buildTypes {
            getByName("debug") {
                applicationIdSuffix = ".debug"
            }
            // R8 (código y recursos) y Baseline Profile (plan.md §7 Hito 11). Sin clave, la release sale
            // sin firmar; la firma la pone CI (docs/release.md).
            getByName("release") {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                if (hasReleaseKey) signingConfig = signingConfigs.getByName("release")
            }
            // Para el uso diario en el teléfono: la release (R8 y perfil) con el id y la firma de debug, así
            // que se instala encima de la debug conservando escritorios y ajustes; perfilable para simpleperf.
            create("fast") {
                initWith(getByName("release"))
                applicationIdSuffix = ".debug"
                signingConfig = signingConfigs.getByName("debug")
                matchingFallbacks += "release"
                isDebuggable = false
                isProfileable = true
            }
        }
        // Los tipos que crea el plugin de Baseline Profile (benchmarkRelease, nonMinifiedRelease…) se
        // instalan en el teléfono para medir: sin clave de release, con la de debug; con clave, el
        // plugin les copia la de release.
        buildTypes.configureEach {
            if (name.startsWith("benchmark") || name.startsWith("nonMinified")) {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
        logger.info("wh01s17: release signing ${if (hasReleaseKey) "configured" else "not configured (unsigned)"}")
    }
}
