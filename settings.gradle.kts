
pluginManagement {
    repositories {
        providers.environmentVariable("HIPKA_MAVEN_MIRROR").orNull?.let { mirror ->
            maven { url = uri(mirror) }
        }
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
        providers.environmentVariable("HIPKA_MAVEN_MIRROR").orNull?.let { mirror ->
            maven { url = uri(mirror) }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "HipkaMusic"
include(":app")
