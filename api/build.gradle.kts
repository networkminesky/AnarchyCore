plugins {
    id("java-library")
    id("maven-publish")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
}

tasks.jar {
    archiveBaseName.set("AnarchyCore-API")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "AnarchyCore-API"
        }
    }
}
