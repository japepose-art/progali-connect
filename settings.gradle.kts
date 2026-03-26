pluginManagement {
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

rootProject.name = "progali-connect"

// Incluimos los módulos indicando su ruta real
include(":app")
project(":app").projectDir = file("android-app/app")

include(":lib-blufi")
project(":lib-blufi").projectDir = file("references/EspBlufiForAndroid/lib-blufi")
