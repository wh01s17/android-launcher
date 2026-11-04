plugins {
    alias(libs.plugins.wh01s17.android.library)
}

dependencies {
    // `JsonElement` aparece en la API: las opciones de cada módulo de la barra son libres.
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
