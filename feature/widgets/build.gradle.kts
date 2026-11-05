plugins {
    alias(libs.plugins.wh01s17.android.library)
    alias(libs.plugins.wh01s17.android.compose)
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:system"))
    implementation(project(":core:fuzzy"))

    testImplementation(libs.junit4)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
