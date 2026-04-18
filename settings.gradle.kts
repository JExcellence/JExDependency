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
include(":JExPlatform")
include(":JExTranslate")
include(":RPlatform")

// JExEconomy with submodules
include(":JExEconomy")
include(":JExEconomy:jexeconomy-api")
include(":JExEconomy:jexeconomy-common")
include(":JExEconomy:jexeconomy-free")
include(":JExEconomy:jexeconomy-premium")

// RCore with submodules
include(":RCore")
//include(":RCore:rcore-common")

// RDQ with submodules
include(":RDQ")
include(":RDQ:rdq-common")
include(":RDQ:rdq-free")
include(":RDQ:rdq-premium")

//RDR with submodules
include(":RDR")
include(":RDR:rdr-common")
include(":RDR:rdr-free")
include(":RDR:rdr-premium")

//RDS with submodules
include(":RDS")
include(":RDS:rds-common")
include(":RDS:rds-free")
include(":RDS:rds-premium")

//RDT with submodules
include(":RDT")
include(":RDT:rdt-common")
include(":RDT:rdt-free")
include(":RDT:rdt-premium")

//RDA with submodules
include(":RDA")
include(":RDA:rda-common")
include(":RDA:rda-free")
include(":RDA:rda-premium")

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

// JExWorkbench with submodules
include(":JExWorkbench")
*/
