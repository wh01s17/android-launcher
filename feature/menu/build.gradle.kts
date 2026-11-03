plugins {
    alias(libs.plugins.wh01s17.android.library)
    alias(libs.plugins.wh01s17.android.compose)
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:apps"))
    implementation(project(":core:fuzzy"))
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
