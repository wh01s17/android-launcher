plugins {
    `kotlin-dsl`
}

group = "com.wh01s17.launcher.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    implementation(libs.ktlint.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.kover.gradlePlugin)
    implementation(libs.tomlj)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    // Las pruebas leen el tema real de ../../theme/wh01s17.
    inputs.dir(rootDir.resolve("../theme/wh01s17"))
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "wh01s17.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "wh01s17.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "wh01s17.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("designTokens") {
            id = "wh01s17.design.tokens"
            implementationClass = "DesignTokensConventionPlugin"
        }
        register("jvmLibrary") {
            id = "wh01s17.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
