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

rootProject.name = "littlekt"

include(":core")

include("wgpu-natives")

include("wgpu-ffm")

include("examples")

include("scene-graph")

include("extensions:tools")

include("extensions:gradle:texturepacker")

include("wgpu-jni:generator")

include("wgpu-jni:core")
