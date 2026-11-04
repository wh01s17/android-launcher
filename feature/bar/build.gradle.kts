plugins {
    alias(libs.plugins.wh01s17.android.library)
    alias(libs.plugins.wh01s17.android.compose)
    alias(libs.plugins.roborazzi)
}

android {
    testOptions {
        unitTests {
            // Robolectric necesita los recursos (fuentes del tema) para dibujar Compose.
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:config"))
    implementation(project(":core:system"))
    implementation(project(":core:input"))
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    debugImplementation(libs.compose.ui.test.manifest)
}

// Las capturas de referencia (src/test/screenshots) forman parte de `check`.
tasks.named("check") {
    dependsOn("verifyRoborazziDebug")
}
