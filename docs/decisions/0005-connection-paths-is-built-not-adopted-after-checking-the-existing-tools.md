---
status: accepted
decision-makers: Maintainers
---

# under-test-external-calls is built, not adopted, after checking the existing tools

**Decided by:** Maintainers · **Status:** accepted (chosen in discussion)

## Context and Problem Statement

The job is to find, from source, a Java program's external connections, the address each one goes to, and which tests
reach them, inside a company, on Windows, without admin rights. Before building further, the existing tools were
checked against that job.

## Considered Options

* **Moderne Prethink** (`io.moderne.recipe:rewrite-prethink`: FindDatabaseConnections, FindExternalServiceCalls,
  FindMessagingConnections, FindTestCoverage): finds connections and maps tests to them, but not addresses; Moderne
  Proprietary licence
* **OpenRewrite, open source** (`org.openrewrite.FindCallGraph` in `org.openrewrite.recipe:rewrite-all`, and
  `org.openrewrite.java.search.FindClassHierarchy` in `rewrite-java`, Apache-2.0): one row per call, with its source
  set and argument types
* **jQAssistant** (GPL-3.0): Cypher queries over a graph read from bytecode; call chains from `@Test` methods only by
  writing the queries; no addresses
* **IBM codeanalyzer-java**: a WALA call graph; JPA only; needs Linux, macOS or WSL
* **Konveyor kantra, Red Hat MTA, Azure AppCAT**: technology tags as file and line matches; no call graph, no tests
* **CodeQL**: models and a call graph; paid for private code
* **Semgrep**: following calls across files is in the paid version only
* **ArchUnit**: no export; calls through interfaces are a known gap
* **Test-impact tools** (Teamscale, OpenClover): need the tests to have run
* **Test-smell tools** (tsDetect, JNose): look only inside the test body
* **Keep building under-test-external-calls on JavaParser**

## Decision Outcome

Chosen in discussion: keep building under-test-external-calls on JavaParser.

OpenRewrite's open-source recipes come closest. On Openfire 4.6.0's `xmppserver` module (788 files, 415 tests), with
the same Maven classpath of 110 jars, OpenRewrite's call graph walked with under-test-external-calls' own rules gives the same
answer on the tests that matter, but not the rest:

| | under-test-external-calls 0.1.0 | OpenRewrite (rewrite-maven-plugin 6.46.1, rewrite-all 1.28.1) |
|---|---|---|
| What it needs | the jar and a classpath file | Maven, its repositories, and a working build |
| Time | 20 seconds, on Java 17 | 1 minute 45 seconds |
| Calls resolved | 54,093 of 54,207 (99.8%) | all, with the compiler's own types; 33 files only partly read |
| Tests reaching a connection from their own body | 122 | the same 122 |
| Tests reaching one through set-up | 40 more | can't be told: no annotations, so set-up and helpers look alike (51 helpers marked as well) |
| Field initialisers | read | no calls recorded for a class body, so a connection opened there is lost |
| Connection points | 54 | 37: only the repo's own class hierarchy, so rules can't match through a library's supertypes (a library `DataSource`, `CloseableHttpClient`) |
| Addresses | can be traced in the syntax tree | nothing to trace them from |

An adapter reading OpenRewrite's tables would have to rebuild most of under-test-external-calls to fill those gaps. The
[comparison](../evaluations/openrewrite-comparison/README.md) can be run again.

### Consequences

* The lists of connection calls borrow from Prethink's documented detectors and Konveyor's technology rules.
* Where an organisation already runs OpenRewrite or Prethink, their tables could later be read as a second source.

## More Information

* Builds on: [It reads source with JavaParser and takes the classpath from the build](0001-it-reads-source-with-javaparser-and-takes-the-classpath-from-the-build.md)
* under-test: [Connections and the test paths to them are found by under-test-external-calls, a JavaParser tool in its own repo](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0072-connections-and-the-test-paths-to-them-are-found-by-under-test-external-calls-a-javaparser-tool-in-its-own-repo.md)
* under-test: [A repo may be built, without running its tests, before the safety gate](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0073-a-repo-may-be-built-without-running-its-tests-before-the-safety-gate.md)
* under-test: [under-test-external-calls does everything a program can decide, and the connection-list skill only what needs judgement](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0074-under-test-external-calls-does-everything-a-program-can-decide-and-the-connection-list-skill-only-what-needs-judgement.md)
