rootProject.name = "AnarchyCore"

include("api")
include("plugin-test")

val jitpackBuild = gradle.startParameter.projectProperties.containsKey("jitpack")
        || System.getenv("JITPACK") == "true"

if (!jitpackBuild) {
    include("plugin")
}
