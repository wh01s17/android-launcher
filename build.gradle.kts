plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// R1–R3 (plan.md §1): longitud de archivos con justificación registrada.
val checkFileLength =
    tasks.register<Exec>("checkFileLength") {
        group = "verification"
        description = "Fails on files over 1000 lines without a justified entry in tools/file-length.allow."
        commandLine("bash", "tools/check-file-length.sh")
    }

tasks.named("check") {
    dependsOn(checkFileLength)
}
