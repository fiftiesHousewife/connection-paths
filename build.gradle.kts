// under-test-external-calls: finds where Java code opens external connections, and which tests reach them, from source.
// The code standards come from build-logic's java-conventions; see AGENTS.md.

plugins {
    id("io.github.fiftieshousewife.java-conventions")
    id("io.github.fiftieshousewife.publishing")
    application
}

// The version is in gradle.properties; a release sets it from its tag with -Pversion.

dependencies {
    implementation(libs.javaparser.symbol.solver)
    implementation(libs.jackson3.databind)
    compileOnly(libs.spotbugs.annotations)
    testCompileOnly(libs.spotbugs.annotations)
    // SpotBugs logs through slf4j-simple; its plugin otherwise picks an older one.
    spotbugsSlf4j(libs.slf4j.simple)
}

// Built with the toolchain's JDK, but runs on Java 17 and later: the machines that run it often have an older JDK.
tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}

application {
    mainClass = "io.github.fiftieshousewife.connectionpaths.Main"
}

// One jar with everything in it: java -jar under-test-external-calls.jar ...
val allInOne = tasks.register<Jar>("allInOneJar") {
    group = "build"
    description = "Builds one runnable jar holding the tool and its libraries."
    archiveFileName = "under-test-external-calls.jar"
    manifest { attributes("Main-Class" to application.mainClass.get(), "Implementation-Version" to project.version) }
    from(sourceSets.main.map { it.output })
    dependsOn(configurations.runtimeClasspath)
    from({ configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) } }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/versions/*/module-info.class", "module-info.class")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.assemble { dependsOn(allInOne) }

// Published as io.github.fiftieshousewife.undertest:under-test-external-calls: the library jar with its POM, and the
// runnable jar beside it with the classifier "all", for networks that only reach a package repository.
publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "under-test-external-calls"
            from(components["java"])
            artifact(allInOne) { classifier = "all" }
            pom {
                name = "under-test-external-calls"
                description = "Finds where Java code opens external connections, and which tests reach them, from source."
            }
        }
    }
}

// Maven Central, through its Central Portal. Needs centralPortalUsername and centralPortalPassword (a Central Portal
// user token), as Gradle properties or ORG_GRADLE_PROJECT_ environment variables.
apply(plugin = "com.gradleup.nmcp.aggregation")
configure<nmcp.NmcpAggregationExtension> {
    centralPortal {
        username = providers.gradleProperty("centralPortalUsername")
        password = providers.gradleProperty("centralPortalPassword")
        publishingType = "AUTOMATIC"
    }
}
dependencies {
    "nmcpAggregation"(project(":"))
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    excludeFilter = layout.projectDirectory.file("config/quality/spotbugs/exclude.xml")
}
