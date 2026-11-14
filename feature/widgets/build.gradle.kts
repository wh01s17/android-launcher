plugins {
    alias(libs.plugins.wh01s17.android.library)
    alias(libs.plugins.wh01s17.android.compose)
}

android {
    // Robolectric necesita los recursos de Android en las pruebas de la vista del widget.
    testOptions { unitTests.isIncludeAndroidResources = true }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:system"))
    implementation(project(":core:fuzzy"))

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
