plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

/** Control del repositorio hecho con un script: falla el build si el script falla. */
fun registerScriptCheck(
    name: String,
    description: String,
    vararg command: String,
) = tasks.register<Exec>(name) {
    group = "verification"
    this.description = description
    commandLine(*command)
}

// R1–R3 (plan.md §1): longitud de archivos con justificación registrada.
val checkFileLength =
    registerScriptCheck(
        "checkFileLength",
        "Fails on files over 1000 lines without a justified entry in tools/file-length.allow.",
        "bash",
        "tools/check-file-length.sh",
    )

// R6 (plan.md §1): colores y dp sólo desde los tokens del tema.
val checkNoLiteralColors =
    registerScriptCheck(
        "checkNoLiteralColors",
        "Fails on literal colors or dp values outside :core:designsystem.",
        "bash",
        "tools/check-no-literal-colors.sh",
    )

// El tema se sincroniza desde ~/dotfiles, no se edita a mano (plan.md §4.1).
val checkTheme =
    registerScriptCheck(
        "checkTheme",
        "Verifies theme/ and the fonts against SHA256SUMS and warns on divergence from the dotfiles.",
        "python3",
        "tools/sync-theme.py",
        "--check",
    )

val testTools =
    registerScriptCheck(
        "testTools",
        "Runs the Python tests of tools/.",
        "python3",
        "-m",
        "unittest",
        "discover",
        "-s",
        "tools/tests",
    )

tasks.named("check") {
    dependsOn(checkFileLength, checkNoLiteralColors, checkTheme, testTools)
    // El generador de tokens y sus pruebas viven en el build incluido.
    dependsOn(gradle.includedBuild("build-logic").task(":convention:check"))
}
