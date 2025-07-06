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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.google.com/") }
        maven { url = uri("https://dl.bintray.com/rikkaw/Shizuku") }
    }
}

rootProject.name = "KeyMapperFoss"
include(":app")
include(":systemstubs")
include(":base")
include(":api")
include(":system")
include(":common")
include(":data")
include(":priv")
