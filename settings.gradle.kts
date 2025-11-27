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
include(":JExTranslate")
include(":RPlatform")

// JExEconomy with submodules
include(":JExEconomy")
include(":JExEconomy:jexeconomy-common")
include(":JExEconomy:jexeconomy-free")
include(":JExEconomy:jexeconomy-premium")

// RCore with submodules
include(":RCore")
include(":RCore:rcore-common")

// RDQ with submodules (v6.0.0 - Modern Java 21+ rebuild)
include(":RDQ")
include(":RDQ:rdq-common")
include(":RDQ:rdq-free")
include(":RDQ:rdq-premium")

// RDQ2 (Testing rank loading fix)
include(":RDQ2")
include(":RDQ2:rdq-common")
include(":RDQ2:rdq-free")
include(":RDQ2:rdq-premium")
