plugins {
    base
}

allprojects {
    group = findProperty("group") as String? ?: "com.raindropcentral"
    version = findProperty("version") as String? ?: "1.0.0"

    repositories {
        mavenCentral()
        mavenLocal()
    }
}