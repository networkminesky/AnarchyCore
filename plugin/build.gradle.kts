plugins {
    id("java-library")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.23"
}

repositories {
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

dependencies {
    implementation(project(":api"))

    paperweight.foliaDevBundle("26.3.build.+")

    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT") {
        isTransitive = false
    }
    compileOnly("org.geysermc.cumulus:cumulus:1.1.2") {
        isTransitive = false
    }

    compileOnly("me.clip:placeholderapi:2.12.3")
    compileOnly("com.github.retrooper:packetevents-spigot:2.14.0")

    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.3")
    implementation("com.google.code.gson:gson:2.11.0")
}

tasks {
    processResources {
        val props = mapOf("version" to project.version)
        filesMatching("plugin.yml") {
            expand(props)
        }
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    jar {
        archiveBaseName.set("AnarchyCore")
        dependsOn(":api:classes")
        from(project(":api").sourceSets.main.get().output)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(
            configurations.runtimeClasspath.get()
                .filter { file ->
                    file.name.contains("HikariCP", ignoreCase = true)
                            || file.name.contains("mariadb", ignoreCase = true)
                            || file.name.contains("gson", ignoreCase = true)
                }
                .map { file -> if (file.isDirectory) file else zipTree(file) },
        )
    }
}
