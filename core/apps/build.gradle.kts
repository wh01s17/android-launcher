plugins {
    alias(libs.plugins.wh01s17.android.library)
    // `AppIcon`: el icono de una app en Compose, compartido por el menú y los tiles.
    alias(libs.plugins.wh01s17.android.compose)
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
