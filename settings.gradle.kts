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

// Simple plugin modules
include(":JExGlow")

// JExEconomy with submodules
include(":JExEconomy")
include(":JExEconomy:jexeconomy-common")
include(":JExEconomy:jexeconomy-free")
include(":JExEconomy:jexeconomy-premium")

// RCore with submodules
include(":RCore")
include(":RCore:rcore-common")

// RDQ with submodules
include(":RDQ")
include(":RDQ:rdq-common")
include(":RDQ:rdq-free")
include(":RDQ:rdq-premium")

/*

// JExHome with submodules
include(":JExHome")
include(":JExHome:jexhome-common")
include(":JExHome:jexhome-free")
include(":JExHome:jexhome-premium")

// JExMultiverse with submodules
include(":JExMultiverse")
include(":JExMultiverse:jexmultiverse-common")
include(":JExMultiverse:jexmultiverse-free")
include(":JExMultiverse:jexmultiverse-premium")

// JExOneblock with submodules
include(":JExOneblock")
include(":JExOneblock:jexoneblock-common")
include(":JExOneblock:jexoneblock-free")
include(":JExOneblock:jexoneblock-premium")
include("RDT")
 */
include("RDS")