plugins {
    alias(libs.plugins.wh01s17.android.library)
}

dependencies {
    // `JsonElement` aparece en la API: las opciones de cada módulo de la barra son libres.
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)
    // `general.layout` es un `TilingLayout` (plan.md §5.2: core/config → core/tiling).
    api(project(":core:tiling"))
    // `bindings` se leen como `Binding` de core/input, con los errores de cada combinación y comando.
    api(project(":core:input"))

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
