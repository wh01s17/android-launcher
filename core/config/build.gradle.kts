plugins {
    alias(libs.plugins.wh01s17.android.library)
}

dependencies {
    // `JsonElement` aparece en la API: las opciones de cada módulo de la barra son libres.
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)
    // `general.layout` es un `TilingLayout` (plan.md §5.2: core/config → core/tiling).
    api(project(":core:tiling"))

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
