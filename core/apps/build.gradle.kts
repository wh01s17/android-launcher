plugins {
    alias(libs.plugins.wh01s17.android.library)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    // Robolectric con gráficos nativos: las pruebas de iconos leen píxeles reales.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
