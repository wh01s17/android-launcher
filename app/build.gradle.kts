plugins {
    alias(libs.plugins.wh01s17.android.application)
    alias(libs.plugins.wh01s17.android.compose)
}

android {
    defaultConfig {
        versionCode = 1
        versionName = "0.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:apps"))
    implementation(project(":core:system"))
    implementation(project(":feature:menu"))
    implementation(project(":feature:settings"))
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Pruebas en el dispositivo (plan.md §9): ./gradlew :app:connectedDebugAndroidTest
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
