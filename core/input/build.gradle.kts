plugins {
    alias(libs.plugins.wh01s17.android.library)
}

dependencies {
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
