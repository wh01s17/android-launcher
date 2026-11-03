plugins {
    alias(libs.plugins.wh01s17.android.library)
    alias(libs.plugins.wh01s17.android.compose)
}

dependencies {
    implementation(project(":core:designsystem"))
}
