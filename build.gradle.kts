plugins {
    id("java-library")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.23"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.helpch.at/releases")
}

dependencies {
    paperweight.foliaDevBundle("26.3.build.+")

    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT") {
        isTransitive = false
    }
    compileOnly("org.geysermc.cumulus:cumulus:1.1.2") {
        isTransitive = false
    }

    compileOnly("me.clip:placeholderapi:2.12.3")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
