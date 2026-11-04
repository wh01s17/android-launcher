plugins {
    alias(libs.plugins.wh01s17.android.application)
    alias(libs.plugins.wh01s17.android.compose)
}

android {
    defaultConfig {
        versionCode = 200
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:apps"))
    implementation(project(":core:config"))
    implementation(project(":core:input"))
    implementation(project(":core:system"))
    implementation(project(":feature:bar"))
    implementation(project(":feature:menu"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:widgets"))
    implementation(project(":feature:workspaces"))
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Pruebas en el dispositivo (plan.md §9): ./gradlew :app:connectedDebugAndroidTest
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
