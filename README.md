# connection-paths

[![Build](https://github.com/fiftiesHousewife/connection-paths/actions/workflows/build.yml/badge.svg)](https://github.com/fiftiesHousewife/connection-paths/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.fiftieshousewife.connectionpaths/connection-paths)](https://central.sonatype.com/artifact/io.github.fiftieshousewife.connectionpaths/connection-paths)
[![Java 17+](https://img.shields.io/badge/java-17%2B-blue)](#running-it)
[![Licence: Apache-2.0](https://img.shields.io/badge/licence-Apache--2.0-blue)](LICENSE)
[![Decisions](https://img.shields.io/badge/decisions-MADR-informational)](docs/decisions/README.md)

**Finds where Java code opens connections to things outside the program (databases, HTTP services, mail, directories,
queues, sockets, other programs), where each one goes, and which tests can reach them, from source alone.**

Before an old repo's tests are run for the first time, you want to know which of them may reach a real database or
service. Searching the tests finds the ones that name a host. It misses the ones that reach a connection through the
production code: a test of `OrderService` that ends up in `JdbcOrderStore`, which opens a database connection three
calls down. connection-paths builds a call graph from the source and walks it back from every place a connection is
opened to the tests, and writes down the chain of calls for each. It traces each connection's address to a host, or to
the setting, environment variable or JNDI name that holds it, and groups the connections into the systems a test may
reach, leaving out those that never leave the machine.

It is one of the tools of [under-test](https://github.com/fiftiesHousewife/under-test), a toolkit for getting old code
properly tested; its connection-list step runs it. It reads and writes files only: it runs none of the code it reads.

## Running it

Java 17 or later. The runnable jar is on Maven Central as
`io.github.fiftieshousewife.connectionpaths:connection-paths`, classifier `all` (so it comes through a package
repository that mirrors Maven Central), and on the [releases](https://github.com/fiftiesHousewife/connection-paths/releases)
page as `connection-paths.jar`. Each comes with a CycloneDX bill of materials.

```bash
java -jar connection-paths.jar --module path/to/module --classpath @classpath.txt --out connection-paths.json
```

`--classpath` gives the jars the code compiles against. Without them most calls into libraries can't be resolved; the
facts file says how many and why. Get them from the build, which resolves dependencies without compiling or running
anything:

| Build | Command |
|---|---|
| Maven | `mvn -q dependency:build-classpath -Dmdep.outputFile=classpath.txt` |
| Gradle | `gradle -q --init-script tools/print-classpath.gradle connectionPathsClasspath` (prints where it wrote the file) |
| Maven, a module with sibling modules | `mvn -q -pl <module> -am -DskipTests compile dependency:build-classpath -Dmdep.outputFile=classpath.txt`: the siblings are built, not published, so the classpath can't be resolved without them |

The classpath from the build is better than one read from the build files by hand, which misses jars that come in
through other jars. If the build can't give one:

* **A wrapper that won't run**: a cloned `mvnw` or `gradlew` may have lost its execute bit; run it as `sh mvnw` (or
  `mvnw.cmd` on Windows).
* **A dependency that can't be downloaded**: old repos often name repositories that have since closed or that refuse
  requests. The build's error names the artifact and the repository; find the artifact in another repository the
  organisation's package repository mirrors.
* **"Access denied" behind a proxy**: Maven sends proxy credentials only after the proxy asks for them, and some
  proxies want them up front. Run the build where the proxy isn't needed, or through the organisation's package
  repository.

`java -jar connection-paths.jar --help` lists every option, including `--main` and `--test` for source folders outside
the usual layout (`--module`, `--main` and `--test` can each be given more than once), `--settings` for folders of
settings files outside the module, `--rules` for more connection rules, and `--max-implementations`.

## What it writes

A JSON file, whose schema is [connection-paths.schema.json](https://github.com/fiftiesHousewife/under-test/blob/main/schema/connection-paths.schema.json) in under-test. Its words (connection point, system, chain, through set-up, shared chain, alternatives, local, test that
starts a framework, dispatch point) mean what under-test's [glossary](https://github.com/fiftiesHousewife/under-test/blob/main/GLOSSARY.md)
says.

| Field | What |
|---|---|
| `summary` | Files and methods read; calls resolved and not; tests, how many may reach a connection, and how many start a framework |
| `connection_points` | Each method that opens or uses a connection: its kind, the rule its call matched, the file and line of that call, its `address`, and, if it stays on this machine, why (`local`) |
| `systems` | What tests may reach outside the program: the points that go to one host, setting, variable, JNDI name or class; the systems that are `alternatives` to it; how many tests may reach it; and the `shared_chain` of calls they all go through |
| `tests` | Each test that may reach a connection: for each kind, a chain of calls from the test to the connection, and whether it goes through what runs with the test (`through_set_up`); and the `systems` it may reach |
| `tests_starting_a_framework` | Each test that starts a framework (`@SpringBootTest`, `@Testcontainers`, `@QuarkusTest`, Spring's runner...), which may connect through wiring no call shows: the annotation, and the class it is on |
| `not_resolved` | The calls it couldn't resolve, by reason, most common first; a missing library shows up here by its types' names |
| `dispatch_points_not_followed` | Calls through an interface or superclass with more implementations than the limit (listeners, handlers, plug-ins): reported, not followed |
| `classpath_notes` | Classpath entries it couldn't use, or read only after a workaround |

An `address` says where the connection's address comes from (`source`) and what it is (`value`): a `literal`, with
`{?}` for parts that can't be known; a `setting`, with the method that reads it (`via`) and, where the repo's
properties, YAML or XML files hold it, its `setting_value`, `setting_file` and `setting_line`; an
`environment-variable`, `system-property` or `jndi-name`; a `classpath-resource`, `local-file` or `dns`; a
`parameter`; or `built-at-run-time`, with the code it reached. `placeholder` marks a value a person is meant to fill
in, such as `https://YOUR-SERVER/`.

The kinds come from [the rules](src/main/resources/connection-rules.txt): `database`, `http`, `mail`, `directory`,
`jndi`, `queue`, `socket`, `url` (a URL may be a file or jar as well as a server), `host-lookup` and `process`.

## How it decides

* Source is read with [JavaParser](https://javaparser.org/) and its symbol solver: the JDK, the module's own source and
  the classpath's jars. A method is known by its type, name and parameter types.
* Calls in lambdas, anonymous classes and local classes belong to the method that holds them. A constructor also runs
  its class's field initialisers, and its superclass's constructor (`super(...)`, or the one without parameters) and
  field initialisers. Any use of a type may run its static initialisers and enum constants. Records are read like
  classes.
* What runs with a test counts as part of it: set-up and tear-down methods (JUnit 3, 4 and 5, and TestNG), the test
  class's constructors and fields, its superclasses', and, for a `@Nested` class, those of the classes around it.
  JUnit 3 tests are found by their superclass. A test a class inherits is listed under each class that runs it, with
  that class's set-up, and not under an abstract class that declares it.
* A call made only to stub or verify a mock (`when(store.all())`, `verify(store).all()`) is not counted as a call.
* A call through an interface or superclass reaches the overriding methods in the source, matched by name and
  parameter types, where a type variable (`save(T)`) matches any type so that generic interfaces work, except:
  * through the JDK's own types (`Map`, `Runnable`...): following them joins every `Map.put` to every class that
    extends a map;
  * where more than three methods in the production source override it (`--max-implementations`; test doubles don't
    count): those are dispatch points, such as "notify every listener", that would otherwise mark nearly every test.
    They are listed instead.
* A rule `Type#method` matches calls to that type and its subtypes. A rule `Type#*` matches, through a subtype,
  only the methods `Type` declares (a template's own setters aren't queries), or every method where it declares
  none, as Spring Data's `Repository` does. A rule `@Annotation` matches calls to the methods of a client that is
  only an interface (`@FeignClient`, Retrofit's `@GET`, `@HttpExchange`), with the annotation's `url` as the
  address.
* An address is traced from the connection call's first argument, or what the call is made on, back through
  variables, fields, constructors, builder chains, the JDK's helpers and the source's own methods, to a literal or a
  name. A call passing a dotted key (`crowd.server.url`) reads a setting, which is looked up in the settings files
  under `--module` and `--settings` folders. See [the decision](docs/decisions/0006-each-connections-address-is-traced-in-the-source-and-looked-up-in-the-repos-settings-files.md).
* A system is the points that go to one host, else to one setting, variable or JNDI name, else those opened in one
  class. Databases in memory or in files, files, jars, classpath resources and localhost are local and part of no
  system; so are host look-ups and processes. Systems reached through the same interface method are alternatives.
  See [the decision](docs/decisions/0007-connection-points-are-grouped-into-systems-and-local-connections-are-part-of-none.md).
* A sibling module's class folder on the classpath (`../i18n/target/classes`) is read from its source.
* A test that starts a framework is listed by the annotation that starts it, on its class, a superclass or a class
  around it. See [the decision](docs/decisions/0009-tests-that-start-a-framework-are-listed-by-their-annotation-apart-from-the-tests-that-reach-a-connection.md).
* A test is listed if it may reach a connection. "May": measurement, the first run of the tests, confirms or clears
  each one.

It does not follow reflection, beans wired only in Spring XML, or code loaded by name at run time. What it doesn't
do yet is in the [backlog](docs/backlog.md).

## Building

`./gradlew check` runs the tests and the code standards; `./gradlew allInOneJar` builds `build/libs/connection-paths.jar`.
GitHub Actions runs both on every pull request and runs the jar on Java 17; a tag such as `v1.2.3` publishes it
(see [the decision](docs/decisions/0002-every-change-is-checked-on-github-actions-and-a-version-tag-publishes-the-jar.md)).
The decisions behind the tool are in [docs/decisions](docs/decisions/README.md); [AGENTS.md](AGENTS.md) is the guide
for AI coding assistants and the people directing them.

## Licence

Apache License 2.0; see [LICENSE](LICENSE) and [NOTICE](NOTICE).
