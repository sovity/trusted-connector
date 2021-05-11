@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

dependencies {
    implementation(project(":ids-api")) { isTransitive = false }
    implementation("org.eclipse.jetty", "jetty-util", libraryVersions["jetty"])
}
