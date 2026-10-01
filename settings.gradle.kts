import org.gradle.api.artifacts.component.ModuleComponentSelector

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
            all {
                val dependency = requested as? ModuleComponentSelector
                if (dependency?.group == "dev.nucleusframework" && dependency.module.startsWith("nucleus.")) {
                    val projectName = dependency.module.removePrefix("nucleus.")
                    if (file("$nucleusPath/$projectName/build.gradle.kts").isFile) {
                        useTarget(project(":$projectName"))
                    }
                }
            }
        }
    }
}
