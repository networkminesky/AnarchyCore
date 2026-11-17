tasks.register("shadowJar") {
    dependsOn(":plugin:jar")
    group = "build"
    description = "Gera plugin/build/libs/AnarchyCore-<versão>.jar com dependências empacotadas."
}

subprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.opencollab.dev/main/")
        maven("https://repo.helpch.at/releases")
        maven {
            name = "GitHubPackagesAnarchyEconomy"
            url = uri("https://maven.pkg.github.com/networkminesky/AnarchyEconomy")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR") ?: ""
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }
}
