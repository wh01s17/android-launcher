// Baseline Profile y Macrobenchmark de la app (plan.md §7 Hito 11, §9). Guía: docs/performance.md.
//
//   ./gradlew :app:generateReleaseBaselineProfile     # regenera app/src/main/generated/baselineProfiles
//   ./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest   # mide arranque y fotogramas
//
// Sólo se usan las variantes de `release`: la app medida es com.wh01s17.launcher, no la del uso
// diario (.debug), así que matar su proceso no toca el launcher ni el servicio de gestos del teléfono.
plugins {
    alias(libs.plugins.wh01s17.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    targetProjectPath = ":app"
}

baselineProfile {
    // El perfil se genera en el dispositivo conectado (el emulador redmi-api35 o el teléfono).
    useConnectedDevices = true
}

androidComponents {
    beforeVariants { variant ->
        variant.enable = variant.buildType?.endsWith("Release") == true
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
}
