# connection-paths

**Finds where Java code opens connections to things outside the program (databases, HTTP services, mail, directories,
queues, sockets, other programs) and which tests can reach them, from source alone.**

Before an old repo's tests are run for the first time, you want to know which of them may reach a real database or
service. Searching the tests finds the ones that name a host. It misses the ones that reach a connection through the
production code: a test of `OrderService` that ends up in `JdbcOrderStore`, which opens a database connection three
calls down. connection-paths builds a call graph from the source and walks it back from every place a connection is
opened to the tests, and writes down the chain of calls for each.

It is one of the tools of [under-test](https://github.com/fiftiesHousewife/under-test), a toolkit for getting old code
properly tested; its connection-list step runs it. It reads and writes files only: it runs none of the code it reads.

## Running it

Java 17 or later.

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

`java -jar connection-paths.jar --help` lists every option, including `--main` and `--test` for source folders outside
the usual layout, `--rules` for more connection rules, and `--max-implementations`.

## What it writes

A JSON file, whose schema is [connection-paths.schema.json](https://github.com/fiftiesHousewife/under-test/blob/main/schema/connection-paths.schema.json) in under-test:

| Field | What |
|---|---|
| `summary` | Files and methods read; calls resolved and not; tests, and how many may reach a connection |
| `connection_points` | Each method that opens or uses a connection: its kind, the call that does it, file and line |
| `tests` | Each test that may reach a connection: for each kind, a chain of calls from the test to the connection, and whether it goes through the test's set-up |
| `not_resolved` | The calls it couldn't resolve, by reason, most common first; a missing library shows up here by its types' names |
| `dispatch_points_not_followed` | Calls through an interface or superclass with more implementations than the limit (listeners, handlers, plug-ins): reported, not followed |
| `classpath_notes` | Classpath entries it couldn't use, or read only after a workaround |

The kinds come from [the rules](src/main/resources/connection-rules.txt): `database`, `http`, `mail`, `directory`,
`jndi`, `queue`, `socket`, `url` (a URL may be a file or jar as well as a server), `host-lookup` and `process`.

## How it decides

* Source is read with [JavaParser](https://javaparser.org/) and its symbol solver: the JDK, the module's own source and
  the classpath's jars. A method is known by its type, name and parameter types.
* Calls in lambdas, anonymous classes and local classes belong to the method that holds them. A constructor also runs
  its class's field initialisers. A test's set-up methods and fields count as part of the test; JUnit 3 tests are
  found by their superclass.
* A call through an interface or superclass reaches the overriding methods in the source, matched by name and number
  of parameters so that generic interfaces work, except:
  * through the JDK's own types (`Map`, `Runnable`...): following them joins every `Map.put` to every class that
    extends a map;
  * where more than three methods override it (`--max-implementations`): those are dispatch points, such as "notify
    every listener", that would otherwise mark nearly every test. They are listed instead.
* A test is listed if it may reach a connection. "May": measurement, the first run of the tests, confirms or clears
  each one.

It does not follow reflection, beans wired only in Spring XML, or code loaded by name at run time.

## Tried on

Openfire 4.6.0's `xmppserver` module (788 files, 415 tests), with its dependency jars: 20 seconds; 99.6% of calls
resolved; 162 tests in 23 classes may reach the database, all through Openfire's `DbConnectionManager`, for instance
through its settings class, which checks the database when the server isn't in set-up mode.

## Building

`./gradlew check` runs the tests and the code standards; `./gradlew allInOneJar` builds `build/libs/connection-paths.jar`.
See [AGENTS.md](AGENTS.md).

## Licence

Apache License 2.0; see [LICENSE](LICENSE) and [NOTICE](NOTICE).
