rootProject.name = "CalendarSeasons"

include(":core", ":adapters:hytale")

// Optional local test plugins: each fixtures/<name> with a build file becomes the project :fixture-<name>.
// They are never shipped, and the mod builds without them.
file("fixtures").listFiles()?.filter { File(it, "build.gradle.kts").exists() }?.sortedBy { it.name }?.forEach {
    include(":fixture-${it.name}")
    project(":fixture-${it.name}").projectDir = it
}
