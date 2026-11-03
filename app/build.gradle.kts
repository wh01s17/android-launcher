plugins {
    alias(libs.plugins.wh01s17.android.application)
    alias(libs.plugins.wh01s17.android.compose)
}

android {
    defaultConfig {
        versionCode = 1
        versionName = "0.0.1"
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
}
