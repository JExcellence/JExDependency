/**
 * Convention for library modules that publish to Maven
 */
plugins {
    id("raindrop.java-conventions")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()
                
                pom {
                    name.set(project.name)
                    description.set(project.description)
                    
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                            distribution.set("repo")
                        }
                    }
                }
            }
        }
        
        repositories {
            mavenLocal()
        }
    }
}

tasks.register("printVersion") {
    group = "help"
    description = "Prints the project version"
    doLast {
        println("Project: ${project.name}")
        println("Version: ${project.version}")
        println("Group: ${project.group}")
    }
}
