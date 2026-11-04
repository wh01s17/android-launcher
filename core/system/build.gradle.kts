plugins {
    alias(libs.plugins.wh01s17.android.library)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    // Robolectric: las fuentes de estado se prueban con difusiones reales del sistema.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
