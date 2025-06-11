rootProject.name = "gif-creator"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots")
        google()
    }
}

// The following block registers dependencies to enable Kobweb snapshot support. It is safe to delete or comment out
// this block if you never plan to use them.
gradle.settingsEvaluated {
    fun RepositoryHandler.kobwebSnapshots() {
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
            content {
                includeGroupByRegex("com\\.varabyte\\.kobweb.*")
            }
            mavenContent {
                snapshotsOnly()
            }
        }
    }

    pluginManagement.repositories {
        kobwebSnapshots()
    }
    @Suppress("UnstableApiUsage")
    dependencyResolutionManagement.repositories {
        kobwebSnapshots()
    }
}

include(":site")
