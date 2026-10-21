// Game-agnostic calendar logic: no game API may be imported here (see checkAdapterBoundary in the root build).
// The unit tests live in core/src/test.
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

tasks.withType<Test> { useJUnitPlatform() }

// Fails if any file under core/ names the Hytale API.
tasks.register("checkAdapterBoundary") {
    group = "verification"
    description = "Fails if core uses the Hytale API."
    val sources = fileTree(projectDir) { include("src/**/*.java") }
    inputs.files(sources)
    val root = rootProject.projectDir
    doLast {
        val offenders = sources.files.filter { f -> f.readLines().any { it.contains("com.hypixel") } }
        if (offenders.isNotEmpty()) {
            throw GradleException("Hytale API used in core:\n" + offenders.joinToString("\n") { " - " + it.relativeTo(root) })
        }
    }
}

tasks.named("check") { dependsOn("checkAdapterBoundary") }
