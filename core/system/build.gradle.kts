plugins {
    alias(libs.plugins.wh01s17.android.library)
}

// Robolectric necesita los textos de `strings.xml` (mensajes de error de los fondos).
android { testOptions { unitTests { isIncludeAndroidResources = true } } }

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    // Robolectric: las fuentes de estado se prueban con difusiones reales del sistema.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
