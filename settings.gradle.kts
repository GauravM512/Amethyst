rootProject.name = "Amethyst"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":composeApp")
include(":nativeEngine")

val nucleusPath = providers.gradleProperty("amethyst.nucleus.path").orNull
    ?: "../Nucleus".takeIf { file("$it/settings.gradle.kts").isFile }
if (nucleusPath != null) {
    includeBuild(nucleusPath) {
        dependencySubstitution {
            substitute(module("dev.nucleusframework:nucleus.decorated-window-tao"))
                .using(project(":decorated-window-tao"))
        }
    }
}
