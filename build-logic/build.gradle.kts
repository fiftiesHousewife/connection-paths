// How this repo is published. The code standards come from the java-conventions plugin, not from here.
plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.gradle.nmcp.plugin)
    implementation(libs.gradle.cyclonedx.plugin)
}
