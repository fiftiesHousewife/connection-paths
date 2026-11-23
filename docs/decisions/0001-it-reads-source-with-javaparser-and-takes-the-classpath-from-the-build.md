---
status: accepted
decision-makers: Maintainers
---

# It reads source with JavaParser and takes the classpath from the build

**Decided by:** Maintainers · **Status:** accepted (chosen in discussion)

## Context and Problem Statement

Finding which tests reach a connection opened deep in production code needs a method-level call graph, made before
the tests have ever run. It must work from source, on old Java, on Windows developer machines without admin rights,
and its output must be text an AI assistant can read.

## Considered Options

* **JavaParser and its symbol solver, in plain Java**
* **Joern**: method-level graphs from source, but Scala, and about 90 MB of libraries or a 1.8 GB distribution
* **CodeQL**: precise without a build, but free only for open-source code
* **Tools that read compiled classes** (java-callgraph, SootUp, WALA): need the code compiled first
* **A class-level graph from searches**: marks most tests, because logging and settings classes join everything up

## Decision Outcome

Chosen in discussion: JavaParser, in a jar that runs on Java 17. The classpath comes from the build, which only
resolves dependencies (`dependency:build-classpath`, or `tools/print-classpath.gradle`); building without running tests
is allowed before a repo's safety gate. Where there is no classpath, it still runs and reports what it couldn't resolve.

Three rules keep the graph from joining everything up: a method is known by its type, name and parameter types, so
`read(Reader)` and `read(URL)` are different methods; calls through the JDK's own types are not followed; and calls
through a type with more than three implementations are dispatch points, reported and not followed.

### Consequences

* The output's schema lives in under-test, which owns the step that reads it.
* Reflection, beans wired only in Spring XML, and code loaded by name are not followed; the first run of the tests
  confirms or clears every test it lists.

## More Information

* under-test: [Connections and the test paths to them are found by connection-paths, a JavaParser tool in its own repo](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0072-connections-and-the-test-paths-to-them-are-found-by-connection-paths-a-javaparser-tool-in-its-own-repo.md)
* under-test: [A repo may be built, without running its tests, before the safety gate](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0073-a-repo-may-be-built-without-running-its-tests-before-the-safety-gate.md)
