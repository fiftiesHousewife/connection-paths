# under-test-external-calls

[![build](https://img.shields.io/github/actions/workflow/status/fiftiesHousewife/under-test-external-calls/build.yml?branch=main&label=build)](https://github.com/fiftiesHousewife/under-test-external-calls/actions/workflows/build.yml)
[![maven central](https://img.shields.io/maven-central/v/io.github.fiftieshousewife.undertest/under-test-external-calls?label=maven%20central)](https://central.sonatype.com/artifact/io.github.fiftieshousewife.undertest/under-test-external-calls)
[![runs on: Java 17+](https://img.shields.io/badge/runs%20on-Java%2017%2B-blue)](#running-it)
[![licence: Apache-2.0](https://img.shields.io/badge/licence-Apache--2.0-blue)](LICENSE)
[![decisions: MADR](https://img.shields.io/badge/decisions-MADR-informational)](docs/decisions/README.md)

**Finds where Java code opens connections to things outside the program (databases, HTTP services, mail, directories,
queues, sockets, other programs), where each one goes, and which tests can reach them, from source alone.**

Before an old repo's tests are run for the first time, you want to know which of them may reach a real database or
service. Searching the tests finds the ones that name a host. It misses the ones that reach a connection through the
production code: a test of `OrderService` that ends up in `JdbcOrderStore`, which opens a database connection three
calls down. under-test-external-calls builds a call graph from the source and walks it back from every place a connection is
opened to the tests, and writes down the chain of calls for each. It traces each connection's address to a host, or to
the setting, environment variable or JNDI name that holds it, and groups the connections into the systems a test may
reach, leaving out those that never leave the machine.

It is one of the tools of [under-test](https://github.com/fiftiesHousewife/under-test), a toolkit for getting old code
properly tested; its connection-list step runs it. It reads and writes files only: it runs none of the code it reads.

## Running it

Java 17 or later. The runnable jar is on Maven Central as
`io.github.fiftieshousewife.undertest:under-test-external-calls`, classifier `all` (so it comes through a package
repository that mirrors Maven Central), and on the [releases](https://github.com/fiftiesHousewife/under-test-external-calls/releases)
page as `under-test-external-calls.jar`. Each comes with a CycloneDX bill of materials.

```bash
java -jar under-test-external-calls.jar --module path/to/module --classpath @classpath.txt --out under-test-external-calls.json
```

`--classpath` gives the jars the code compiles against. Without them most calls into libraries can't be resolved; the
facts file says how many and why. Get them from the build, which compiles the code and the tests first, so the
classpath is the one that compile used (a module of the same build can't be resolved until it is built); no test
runs:

| Build | Command |
|---|---|
| Maven | `mvn -q -pl <module> -am test-compile dependency:build-classpath -Dmdep.includeScope=test -Dmdep.outputFile=classpath.txt` |
| Gradle | `gradle -q --init-script tools/print-classpath.gradle underTestExternalCallsClasspath` (compiles the tests and runs none; prints where it wrote the file) |

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

`java -jar under-test-external-calls.jar --help` lists every option, including `--main` and `--test` for source folders outside
the usual layout (`--module`, `--main` and `--test` can each be given more than once), `--settings` for folders of
settings files outside the module, `--rules` for more connection rules, and `--max-implementations`.

## What it writes

A JSON file, whose schema is [external-calls.schema.json](https://github.com/fiftiesHousewife/under-test/blob/main/schema/external-calls.schema.json) in under-test. Its words (connection point, system, chain, through set-up, shared chain, alternatives, local, test that
starts a framework, dispatch point) mean what under-test's [glossary](https://github.com/fiftiesHousewife/under-test/blob/main/GLOSSARY.md)
says.

| Field | What |
|---|---|
| `summary` | Files and methods read; calls resolved and not; tests, how many may reach a connection, and how many start a framework |
| `connection_points` | Each call that opens or uses a connection, so a method that connects twice is listed twice: its kind, the rule its call matched, its method, the file and line of the call, its `address`, and, if it stays on this machine, why (`local`) |
| `systems` | What tests may reach outside the program: the points that go to one host, setting, variable, JNDI name or class; the systems that are `alternatives` to it; how many tests may reach it; and the `shared_chain` of calls they all go through |
| `tests` | Each test that may reach a connection: for each kind, a chain of calls from the test to the connection; whether it goes through what runs with the test (`through_set_up`); the first step that would run a mock the test puts in place instead of the real code (`through_mock`); where the test's own settings send it (`address_for_tests`: `host`, `local`, `none` or `run-time`); the `systems` it may reach; and the servers on this machine its rules, extensions, Testcontainers containers, set-up or tests start, directly or through helpers (`stand_ins`) |
| `tests_starting_a_framework` | Each test that starts a framework (`@SpringBootTest`, `@Testcontainers`, `@QuarkusTest`, Spring's runner...), which may connect through wiring no call shows: the annotation, and the class it is on |
| `not_resolved` | The calls it couldn't resolve, by reason, most common first; a missing library shows up here by its types' names |
| `dispatch_points_not_followed` | Calls through an interface or superclass with more implementations than the limit (listeners, handlers, plug-ins): reported, not followed |
| `classpath_notes` | Classpath entries it couldn't use, or read only after a workaround |

An `address` says where the connection's address comes from (`source`) and what it is (`value`): a `literal`, with
`{?}` for parts that can't be known; a `setting`, with the method that reads it (`via`) and, where the repo's
properties, YAML or XML files hold it, its `setting_value`, `setting_file` and `setting_line`; an
`environment-variable`, `system-property` or `jndi-name`; a `classpath-resource`, `local-file` or `dns`; a
`parameter`; or `built-at-run-time`, with the code it reached. `placeholder` marks a value a person is meant to fill
in, such as `https://YOUR-SERVER/`. A setting read as `${key:default}` has its default as its `setting_value`, with no
`setting_file`, until a file sets it; a file's value that refers to an environment variable, `${DB_URL:jdbc:h2:mem:x}`,
is its default, and with none, `${DB_URL}`, a test's address is known only at run time.

The kinds come from [the rules](src/main/resources/connection-rules.txt): `database`, `http`, `mail`, `directory`,
`jndi`, `queue`, `socket`, `url` (a URL may be a file or jar as well as a server), `host-lookup` and `process`.

## How it decides

* Source is read with [JavaParser](https://javaparser.org/) and its symbol solver: the JDK, the module's own source and
  the classpath's jars. A method is known by its type, name and parameter types.
* Calls in lambdas and local classes belong to the method that holds them. An anonymous class is a type of its own,
  named as the compiler names it (`Watchers$1`): its methods run when they are called through what it implements,
  not where it is made, so a listener registered in a static initialiser isn't run when the class
  loads. A constructor also runs
  its class's field initialisers, and its superclass's constructor (`super(...)`, or the one without parameters) and
  field initialisers. Any use of a type may run its static initialisers and enum constants. Records are read like
  classes.
* What runs with a test counts as part of it: set-up and tear-down methods (JUnit 3, 4 and 5, and TestNG), the test
  class's constructors and fields, its superclasses', and, for a `@Nested` class, those of the classes around it.
  JUnit 3 tests are found by their superclass. A test a class inherits is listed under each class that runs it, with
  that class's set-up, and not under an abstract class that declares it. A test's chain for each kind starts in its
  own body, unless that goes to an address known only at run time and its set-up connects the same kind to a host or
  to this machine, such as a client the set-up connects and the test then uses.
* A call made only to stub or verify a mock (`when(store.all())`, `verify(store).all()`) is not counted as a call.
* A `super.m()` call runs only the method it names, never one that overrides it.
* A call through an interface or superclass reaches only the overriding methods of classes the program creates (with
  `new`, as an enum or test class, by a framework annotation, or by a name in the source or the settings). A `new`
  in a class's own code counts only once that class is created, or in static code; one in a test class's code counts
  only for that class's tests. See
  [the decision](docs/decisions/0011-a-call-through-an-interface-reaches-only-the-classes-the-program-creates.md).
  They are matched by name and parameter types, where a type variable (`save(T)`) matches any type so that generic interfaces work, except:
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
  under `--module` and `--settings` folders: production settings before test ones, a file without a Spring profile
  before one with, and keys matched loosely, as Spring matches them (`jdbcUrl`, `jdbc-url`, `JDBC_URL`). A field
  of a `@ConfigurationProperties("orders-db")` class reads the setting `orders-db.<field>`. A Spring Data repository's
  call reads `spring.datasource.url`; where no settings file sets it, the tool takes the embedded database Spring
  Boot starts when H2, Derby or HSQLDB is on the classpath. See [the decision](docs/decisions/0006-each-connections-address-is-traced-in-the-source-and-looked-up-in-the-repos-settings-files.md).
* A system is the points that go to one host, else to one setting, variable or JNDI name, else those opened in one
  class. Databases in memory or in files, files, jars, classpath resources and localhost are local and part of no
  system (a look-up on the machine's DNS servers is not: they are other machines); so are host look-ups and processes. A look-up of a setting that names an address of this machine to listen
  on (`network.interface`) is local too, and so is a look-up of a numeric address, which looks nothing up. An address
  that can't be followed, such as a parameter no code in the source passes, is never local: in a library, its public
  entry points are systems named by their class. Systems reached through the same interface method are alternatives.
  See [the decision](docs/decisions/0007-connection-points-are-grouped-into-systems-and-local-connections-are-part-of-none.md).
* A chain that calls an instance method of a type the test mocks (a `@Mock` field, or `mock(T.class)` in the test,
  what runs with it, or a test-source helper they call) is kept, and marked at that step. Where the test would
  connect comes from the settings on the test classpath, test resources before main ones: a setting none of
  them gives means the test can't reach the system. See [the decision](docs/decisions/0010-each-reach-says-whether-it-runs-a-mock-and-where-the-tests-own-settings-send-it.md).
  A path such as `/owners/1`, in a test that serves the application on a port
  (`@SpringBootTest(webEnvironment = RANDOM_PORT)` or `DEFINED_PORT`), goes to it on this machine, and is local.
  Where a reach's own address leaves it unknown, the test's own call into its chain is traced, and taken if it
  names a host or this machine: `Jsoup.connect("https://jsoup.org/").get()` hands the address to a builder that
  holds it until the connection is made. A method that returns what it built from a parameter gives the argument
  its call passes, and fluent `url(...)` and `uri(...)` setters set an address.
  A point whose own address is a parameter takes the most informative of what the production code calling it
  passes, caller by caller; each test's reach follows its own chain instead.
  A point whose address is a parameter is followed back along each test's chain to the argument its caller passes;
  a JNDI environment table's address is its `Context.PROVIDER_URL`, and a value built in ways that can't be
  followed comes from the settings-backed fields its method reads. Code that runs only once a database connection is
  open (a method given a `java.sql` connection, statement or result set, a call passed a value read from one, or one
  inside a loop or `if` on one, such as `while (rows.next())`) can't connect in a test whose database reach can't:
  it takes that reach's `address_for_tests`, and its mock. A DNS look-up through JNDI is where it asks for records
  (`getAttributes`, `search`), not where its context is made.
* A test that starts a framework is listed by the annotation that starts it, on its class, a superclass or a class
  around it. See [the decision](docs/decisions/0009-tests-that-start-a-framework-are-listed-by-their-annotation-apart-from-the-tests-that-reach-a-connection.md).
* A test is listed if it may reach a connection. "May": measurement, the first run of the tests, confirms or clears
  each one.

* A bean defined in Spring XML whose class is a connection client (a `DataSource` with a `url` property) is a
  connection point at the XML file and line, named `<class>#<bean id>`, with that property as its address. A test
  class whose `@ContextConfiguration` (or `@SpringJUnitConfig`, `@ImportResource`) names the file reaches its beans
  through its set-up, and code that makes a `ClassPathXmlApplicationContext` on it reaches them from where it does;
  files are matched by name, through a step of their own, `ApplicationContext#<xml applicationContext.xml>`, and a
  file's `<import resource=...>` makes the imported file's beans too.

It does not follow reflection, the wiring between Spring beans, or code loaded by name at run time. What it doesn't
do yet is in the [backlog](docs/backlog.md).

## Building

`./gradlew check` runs the tests and the code standards; `./gradlew allInOneJar` builds `build/libs/under-test-external-calls.jar`.
GitHub Actions runs both on every pull request and runs the jar on Java 17; a tag such as `v1.2.3` publishes it
(see [the decision](docs/decisions/0002-every-change-is-checked-on-github-actions-and-a-version-tag-publishes-the-jar.md)).
The decisions behind the tool are in [docs/decisions](docs/decisions/README.md); [AGENTS.md](AGENTS.md) is the guide
for AI coding assistants and the people directing them.

## Licence

Apache License 2.0; see [LICENSE](LICENSE) and [NOTICE](NOTICE).
