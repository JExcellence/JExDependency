pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
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

rootProject.name = "RaindropPlugins"

// Simple library modules
include(":JExCommand")
include(":JExDependency")
include(":JExEconomy")
include(":JExTranslate")
include(":RPlatform")

// RCore with submodules
include(":RCore")
include(":RCore:rcore-common")
include(":RCore:rcore-free")
include(":RCore:rcore-premium")

// RDQ with submodules
include(":RDQ")
include(":RDQ:rdq-common")
include(":RDQ:rdq-free")
include(":RDQ:rdq-premium")
