rootProject.name = "AnarchyCore"

include("api")

val jitpackBuild = gradle.startParameter.projectProperties.containsKey("jitpack")
        || System.getenv("JITPACK") == "true"

if (!jitpackBuild) {
    include("plugin")
}
