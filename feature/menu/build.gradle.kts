plugins {
    alias(libs.plugins.wh01s17.android.library)
    alias(libs.plugins.wh01s17.android.compose)
    alias(libs.plugins.roborazzi)
}

android {
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:apps"))
    implementation(project(":core:fuzzy"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.named("check") {
    dependsOn("verifyRoborazziDebug")
}
