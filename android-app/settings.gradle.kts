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
include(":app")
include(":lib-blufi")

project(":lib-blufi").projectDir = file("../references/EspBlufiForAndroid/lib-blufi")
