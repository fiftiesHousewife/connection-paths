---
status: proposed
decision-makers: Maintainers
---

# Every change is checked on GitHub Actions, and a version tag publishes the jar

**Decided by:** Maintainers · **Status:** proposed

## Context and Problem Statement

`./gradlew check` holds the code standards and the example repo's known answers, but runs only when someone runs it.
The jar is built with Java 25 for Java 17, and the tests run on Java 25. under-test's connection-list step needs a jar
it can download by version.

## Considered Options

* **GitHub Actions, the same as under-test**: `./gradlew check` on every push to `main` and every pull request; the
  jar run on Java 17 against the example repo; a version tag publishes the jar, only after those pass
* **Checks on people's machines only**: nothing to set up, but nothing stops a change that fails them

## Decision Outcome

Proposed: GitHub Actions, the same as under-test and under-test-airgap:

| Workflow | Job | What |
|---|---|---|
| [Build](../../.github/workflows/build.yml) | `check` | `./gradlew check allInOneJar` on Java 25; keeps the reports and the jar |
| | `runs-on-java-17` | Runs the jar on Java 17 against the example repo, and checks it wrote a facts file |
| [Release](../../.github/workflows/release.yml) | `check` | The Build workflow, for a tag such as `v1.2.3` |
| | `publish` | Only if it passed: publishes to Maven Central and makes a GitHub release, as [it is published to Maven Central with the runnable jar beside it, signed, with a bill of materials](0004-it-is-published-to-maven-central-with-the-runnable-jar-beside-it-signed-with-a-bill-of-materials.md) says |

Dependabot proposes updates to the build's libraries and plugins and to the pipeline's actions, by pull request.

### Consequences

* `check` is made a required check on `main` in the repository's settings.
* A release is a tag such as `v0.1.0`; the release sets the version from it, and `gradle.properties` holds the next one.
* The README shows the build's state.

## More Information

* Builds on: [It reads source with JavaParser and takes the classpath from the build](0001-it-reads-source-with-javaparser-and-takes-the-classpath-from-the-build.md)
