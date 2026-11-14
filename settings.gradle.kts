pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "wh01s17-launcher"

include(":app")

include(
    ":core:designsystem",
    ":core:tiling",
    ":core:config",
    ":core:apps",
    ":core:system",
    ":core:input",
    ":core:fuzzy",
)

include(
    ":feature:workspaces",
    ":feature:bar",
    ":feature:menu",
    ":feature:notifications",
    ":feature:widgets",
    ":feature:settings",
    ":feature:screensaver",
)

// Baseline Profile y Macrobenchmark de la app (plan.md §7 Hito 11).
include(":benchmark")
