pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://repo.tcoded.com/releases")
        maven("https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://repo.tcoded.com/releases")
        maven("https://jitpack.io")
    }
}

rootProject.name = "RCore"

include(
    ":rcore-common",
    ":rcore-free",
    ":rcore-premium",
)