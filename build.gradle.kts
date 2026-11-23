// connection-paths: finds where Java code opens external connections, and which tests reach them, from source.
// The code standards come from build-logic's java-conventions; see AGENTS.md.

plugins {
    id("io.github.fiftieshousewife.java-conventions")
    application
}

version = "0.1.0"

dependencies {
    implementation(libs.javaparser.symbol.solver)
    implementation(libs.jackson3.databind)
}

// Built with the toolchain's JDK, but runs on Java 17 and later: the machines that run it often have an older JDK.
tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}

application {
    mainClass = "io.github.fiftieshousewife.connectionpaths.Main"
}

// One jar with everything in it: java -jar connection-paths.jar ...
val allInOne = tasks.register<Jar>("allInOneJar") {
    group = "build"
    description = "Builds one runnable jar holding the tool and its libraries."
    archiveFileName = "connection-paths.jar"
    manifest { attributes("Main-Class" to application.mainClass.get(), "Implementation-Version" to project.version) }
    from(sourceSets.main.map { it.output })
    dependsOn(configurations.runtimeClasspath)
    from({ configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) } }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/versions/*/module-info.class", "module-info.class")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.assemble { dependsOn(allInOne) }

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    excludeFilter = layout.projectDirectory.file("config/quality/spotbugs/exclude.xml")
}
