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
    implementation(project(":core:apps"))
    implementation(project(":feature:menu"))
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
}
