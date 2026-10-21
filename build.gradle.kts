import java.util.zip.ZipFile

// Calendar & Seasons: a game-agnostic core plus one adapter per game (adapters/hytale is the Hytale mod).
// This file holds what every project shares; the plugin jar and the release live in adapters/hytale.
val modVersion = "1.0.0"

allprojects {
    version = modVersion
    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "java")
    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }
    tasks.withType<JavaCompile> { options.encoding = "UTF-8" }
}

// Local test plugins (fixtures/*) compile against the Hytale adapter and the core; they are never shipped.
configure(subprojects.filter { it.name.startsWith("fixture-") }) {
    val hytaleServerJar = (findProperty("hytaleServerJar") as String?)
        ?: "${System.getenv("APPDATA")}/Hytale/install/release/package/game/latest/Server/HytaleServer.jar"
    dependencies {
        "compileOnly"(files(hytaleServerJar))
        "compileOnly"(project(":core"))
        "compileOnly"(project(":adapters:hytale"))
    }
    tasks.named<Jar>("jar") {
        archiveBaseName.set(project.name)
        archiveVersion.set("")
        destinationDirectory.set(rootProject.layout.buildDirectory.dir(if ((findProperty("flavour") as String?) == "naive") "mods-naive" else "mods"))
    }
}

tasks.register("release") {
    group = "distribution"
    description = "Builds build/release/CalendarSeasons-<version>.jar and .zip."
    dependsOn(":adapters:hytale:release")
}

// ---------------------------------------------------------------------------------------------------------
// checkNoProjectMarks: no published file (tracked files, the jar, the mod page text) may contain
// blocked terms. The words are assembled at run time so that this file doesn't match itself.
// ---------------------------------------------------------------------------------------------------------
val checkNoProjectMarks = tasks.register("checkNoProjectMarks") {
    group = "verification"
    description = "Fails if a tracked or shipped file contains a blocked term."
    dependsOn(":adapters:hytale:jar")
    val jarFile = project(":adapters:hytale").tasks.named<Jar>("jar").flatMap { it.archiveFile }
    val root = projectDir
    val fallback = fileTree(".") { include("*.md", "*.kts", "*.properties", "LICENSE", "core/src/main/**", "core/*.kts", "adapters/*/src/main/**", "adapters/*/*.kts") }
    val pageKit = fileTree("docs/page") { exclude("**/*.png") }
    doLast {
        val marks = Regex(listOf("arch" + "mage", "spell" + "bound", "master" + "\\s+of\\s+" + "magic").joinToString("|"), RegexOption.IGNORE_CASE)
        val hits = mutableListOf<String>()
        // Files git would track (outside git: the public paths), plus the mod page text.
        val tracked: List<File> = try {
            val p = ProcessBuilder("git", "ls-files", "--cached", "--others", "--exclude-standard").directory(root).start()
            val out = p.inputStream.bufferedReader().readText()
            if (p.waitFor() != 0) throw IllegalStateException("git failed")
            out.lines().filter { it.isNotBlank() }.map { File(root, it) }
        } catch (e: Exception) {
            fallback.files.toList()
        }
        for (f in tracked + pageKit.files) {
            if (!f.isFile || f.name.endsWith(".jar") || f.name.endsWith(".png")) continue
            f.readLines().forEachIndexed { i, line -> if (marks.containsMatchIn(line)) hits += "${f.relativeTo(root)}:${i + 1}" }
        }
        ZipFile(jarFile.get().asFile).use { zf ->
            for (e in zf.entries()) {
                if (e.isDirectory) continue
                if (marks.containsMatchIn(e.name)) hits += "jar:${e.name}"
                val text = String(zf.getInputStream(e).readAllBytes(), Charsets.ISO_8859_1)
                if (marks.containsMatchIn(text)) hits += "jar:${e.name} (content)"
            }
        }
        if (hits.isNotEmpty()) throw GradleException("Project marks found:\n" + hits.joinToString("\n") { " - $it" })
    }
}

tasks.register("check") {
    group = "verification"
    dependsOn(checkNoProjectMarks)
    dependsOn(":core:check", ":adapters:hytale:check")
}
