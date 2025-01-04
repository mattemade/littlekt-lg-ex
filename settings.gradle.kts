pluginManagement {
    includeBuild("convention-plugins")
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0" }

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "littlekt-lg-ex"

include(":core")

include("samples")

include("extensions:tools")

include("extensions:gradle:texturepacker")

include(":game-utils")
include(":game")
include(":bossrush")
