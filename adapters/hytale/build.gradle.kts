import java.util.zip.ZipFile

// The Hytale plugin: one jar that carries the adapter and the shaded core (classes and calendar presets).
// The server jar is the API: compile-only, never bundled. Its path is per machine (hytaleServerJar property).
val hytaleServerJar = (findProperty("hytaleServerJar") as String?)
    ?: "${System.getenv("APPDATA")}/Hytale/install/release/package/game/latest/Server/HytaleServer.jar"

val rootDir = rootProject.projectDir

// -Pflavour=naive builds a local test variant (never published) from the extra sources in fixtures/naive.
val naive = (findProperty("flavour") as String?) == "naive"
val naiveFixture = rootDir.resolve("fixtures/naive")
if (naive && !naiveFixture.resolve("java").isDirectory) throw GradleException("-Pflavour=naive needs fixtures/naive, which is local only")

dependencies {
    "compileOnly"(files(hytaleServerJar))
    "implementation"(project(":core"))
}

tasks.named<Jar>("jar") {
    destinationDirectory.set(rootProject.layout.buildDirectory.dir(if (naive) "mods-naive" else "mods"))
    archiveBaseName.set(if (naive) "CalendarSeasons-naive" else "CalendarSeasons")
    from("$rootDir/LICENSE")
    // Shade the core in: players install one jar.
    from(project(":core").the<SourceSetContainer>().named("main").map { it.output })
}

// The naive flavour swaps the config store, the main class (no settings hook) and the sidecar; everything
// else is shared. Its sources are merged into build/naive-src, because a source-set exclude would also drop
// the replacement files.
if (naive) {
    val naiveSrc = layout.buildDirectory.dir("naive-src")
    val mergeNaive = tasks.register<Sync>("mergeNaiveSources") {
        from("src/main/java") {
            exclude("calendarseasons/adapter/FlavourStore.java", "calendarseasons/adapter/CalendarSeasonsPlugin.java")
            into("java")
        }
        from(naiveFixture.resolve("java")) { into("java") }
        from("src/main/resources") {
            exclude("Settings/**")
            into("resources")
        }
        from(naiveFixture.resolve("resources")) { into("resources") }
        into(naiveSrc)
    }
    the<SourceSetContainer>().named("main") {
        java.setSrcDirs(listOf(naiveSrc.map { it.dir("java") }))
        resources.setSrcDirs(listOf(naiveSrc.map { it.dir("resources") }))
    }
    tasks.named("compileJava") { dependsOn(mergeNaive) }
    tasks.named("processResources") { dependsOn(mergeNaive) }
}

tasks.named<ProcessResources>("processResources") {
    val v = project.version.toString()
    inputs.property("version", v)
    filesMatching("manifest.json") { expand("version" to v) }
}

// The Hytale API may be named only in calendarseasons.adapter (the plugin's main class lives there too).
// A Hytale update then breaks one package. core/ has its own check that it names no Hytale API at all.
val checkAdapterBoundary = tasks.register("checkAdapterBoundary") {
    group = "verification"
    description = "Fails if code outside calendarseasons.adapter uses the Hytale API."
    val sources = fileTree("src/main/java") { include("**/*.java") } + fileTree(naiveFixture.resolve("java")) { include("**/*.java") }
    inputs.files(sources)
    doLast {
        val offenders = sources.files.filter { f ->
            !f.invariantSeparatorsPath.contains("/calendarseasons/adapter/") && f.readLines().any { it.contains("com.hypixel") }
        }
        if (offenders.isNotEmpty()) {
            throw GradleException("Hytale API used outside calendarseasons.adapter:\n" + offenders.joinToString("\n") { " - " + it.relativeTo(rootDir) })
        }
    }
}

// The jar must never carry Hytale classes, the server jar, copied game assets or decompiled code.
val checkJarContents = tasks.register("checkJarContents") {
    group = "verification"
    description = "Fails if the plugin jar contains Hytale classes, source files or anything outside the allow list."
    dependsOn(tasks.named("jar"))
    val jarFile = tasks.named<Jar>("jar").flatMap { it.archiveFile }
    doLast {
        val allowed = Regex("""META-INF/.*|manifest\.json|LICENSE|calendarseasons/.*\.class|calendars/[a-z]+\.json|Settings/Hledjolf_CalendarSeasons\.settings\.json""")
        val names = ZipFile(jarFile.get().asFile).use { zf ->
            zf.entries().asSequence().filter { !it.isDirectory }.map { it.name }.toSet()
        }
        val bad = names.filter { !allowed.matches(it) }
        if (bad.isNotEmpty()) throw GradleException("Unexpected files in the jar:\n" + bad.joinToString("\n") { " - $it" })
        // Core classes and the presets must be inside: players install one jar.
        for (need in listOf("calendarseasons/core/DateMath.class", "calendarseasons/app/CalendarService.class", "calendars/harvest.json")) {
            if (need !in names) throw GradleException("The jar is missing $need (core was not merged in)")
        }
    }
}

tasks.named("check") {
    dependsOn(checkAdapterBoundary)
    dependsOn(checkJarContents)
}

// ---------------------------------------------------------------------------------------------------------
// release: <root>/build/release/CalendarSeasons-<version>.jar and CalendarSeasons-<version>.zip (the jar plus
// README.md, LICENSE and CHANGELOG.md). Runs check first and verifies the jar. With -Pflavour=naive it builds
// the local test variant into build/release-naive and copies the jar to fixtures/naive.
// ---------------------------------------------------------------------------------------------------------
val releaseDir = rootProject.layout.buildDirectory.dir(if (naive) "release-naive" else "release")
val baseName = if (naive) "CalendarSeasons-naive" else "CalendarSeasons"

val releaseJar = tasks.register<Copy>("releaseJar") {
    group = "distribution"
    description = "Copies the checked plugin jar to the release folder."
    dependsOn(tasks.named("check"))
    dependsOn(rootProject.tasks.named("checkNoProjectMarks"))
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(releaseDir)
    val ver = project.version.toString()
    doLast {
        val jar = releaseDir.get().file("$baseName-$ver.jar").asFile
        ZipFile(jar).use { zf ->
            val names = zf.entries().asSequence().map { it.name }.toList()
            val bad = names.filter { it.startsWith("cstest/") || it.startsWith("com/hypixel/") || it.contains("Test") && it.endsWith(".class") || it.endsWith(".java") }
            if (bad.isNotEmpty()) throw GradleException("Release jar contains fixture, test or Hytale files: $bad")
            val manifest = String(zf.getInputStream(zf.getEntry("manifest.json")).readAllBytes(), Charsets.UTF_8)
            if (!manifest.contains("\"Version\": \"$ver\"")) throw GradleException("manifest.json version does not match $ver")
        }
        if (naive) jar.copyTo(naiveFixture.resolve(jar.name), overwrite = true)
    }
}

tasks.register<Zip>("release") {
    group = "distribution"
    description = "Builds build/release/CalendarSeasons-<version>.jar and .zip."
    dependsOn(releaseJar)
    archiveFileName.set("$baseName-${project.version}.zip")
    destinationDirectory.set(releaseDir)
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    from(rootDir) { include("README.md", "LICENSE", "CHANGELOG.md") }
    doLast { logger.lifecycle("Release: " + releaseDir.get().asFile) }
}
