plugins {
    id("java-library")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.23"
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
}

tasks {
    processResources {
        val props = mapOf("version" to project.version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    jar {
        archiveBaseName.set("AnarchyCore")
    }
}
