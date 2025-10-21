pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://nexus.neetgames.com/repository/maven-releases/")
        maven("https://repo.auxilor.io/repository/maven-public/")
        maven("https://repo.tcoded.com/releases")
        maven("https://jitpack.io")
    }
}

rootProject.name = "RDQ"

include(
    "rdq-common",
    "rdq-free",
    "rdq-premium",
)