plugins {
    id("java-library")
}

dependencies {
    testImplementation(project(":plugin"))
    testImplementation(project(":api"))
    testImplementation("io.papermc.paper:paper-api:26.3.build.+")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.h2database:h2:2.3.232")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}