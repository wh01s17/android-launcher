import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension

plugins {
    alias(libs.plugins.wh01s17.jvm.library)
}

dependencies {
    // Sólo para leer y escribir el árbol como JSON; sigue siendo Kotlin puro (plan.md R7).
    // `WorkspaceCodec` devuelve y recibe `JsonElement`: forma parte de la API.
    api(libs.kotlinx.serialization.json)
}

// R7: el motor de tiling exige al menos un 90 % de cobertura de líneas, y `check` lo comprueba.
extensions.configure<KoverProjectExtension> {
    reports {
        verify {
            rule {
                minBound(90)
            }
        }
    }
}

tasks.named("check") {
    dependsOn("koverVerify")
}
