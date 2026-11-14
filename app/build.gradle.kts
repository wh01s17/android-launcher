plugins {
    alias(libs.plugins.wh01s17.android.application)
    alias(libs.plugins.wh01s17.android.compose)
    alias(libs.plugins.baselineprofile)
}

android {
    // versionCode y versionName salen de version.properties (plan.md §8.1, build-logic).
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

baselineProfile {
    // Un solo perfil en src/main para todas las variantes: también la `fast` del uso diario.
    mergeIntoMain = true
    // Se regenera a mano (docs/performance.md), no en cada compilación de release.
    automaticGenerationDuringBuild = false
}

dependencies {
    // Instala el Baseline Profile al instalar por ADB o Obtainium (sin Play Store, plan.md §8.2).
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":benchmark"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:apps"))
    implementation(project(":core:config"))
    implementation(project(":core:input"))
    implementation(project(":core:system"))
    implementation(project(":feature:bar"))
    implementation(project(":feature:menu"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:screensaver"))
    implementation(project(":feature:widgets"))
    implementation(project(":feature:workspaces"))
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Fugas de memoria en la debug (plan.md §7 Hito 11); nunca en fast ni release.
    debugImplementation(libs.leakcanary.android)

    // Pruebas en el dispositivo (plan.md §9): ./gradlew :app:connectedDebugAndroidTest
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
