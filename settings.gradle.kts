pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    // gradle/libs.versions.toml is discovered automatically as the "libs" catalog —
    // no explicit versionCatalogs { from(...) } needed here.
}

rootProject.name = "AlphaDragon"

include(":app")

// Core shared modules
include(":core:common")
include(":core:payment-interface")
include(":core:testing")

// Merchant modules (Phase 2)
include(":merchant:worldpay")
include(":merchant:fiserv")
include(":merchant:firstdata")
include(":merchant:clover")
include(":merchant:cardnet")
include(":merchant:elavon")
include(":merchant:manya")

// Terminal modules (Phase 2)
include(":terminal:ingenico")
include(":terminal:clover-flex")
include(":terminal:castles")
