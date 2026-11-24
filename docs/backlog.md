# Backlog

What under-test-external-calls doesn't do yet, most useful first. Each item says what it would change and how to
tell it's done. A behaviour that is built gets a case in the example repo, as [AGENTS.md](../AGENTS.md) says; a
choice between ways of building it gets a [decision record](decisions/README.md).

## What it finds

1. **An address held in an object's field, for the point itself.** A test's own call now gives its reach the
   address it hands a builder (`Jsoup.connect(url)`), and a field's stand-in value (`http://undefined/`) gives way
   to what its setter is given, but the point's `address` still names the setter's parameter: following that
   parameter back to what the setter's callers pass would give the point the addresses they set. For a library, a system named after its connecting class says
   little: the hosts are its callers'.
2. **A server's instance only a running application has.** `XMPPServer.getInstance()` is null in a test that
   starts no server, so a chain through it doesn't run (Openfire's GroupJIDTest).
3. **Spring XML read as Spring reads it.** A test or code that loads a context now reaches the beans in the file it
   names, matched by file name. Still to do, each a rule a program can follow:
   * a context named on a meta-annotation of the repo's own, or by Spring's JUnit 3 base classes
     (`AbstractDependencyInjectionSpringContextTests` and the like) in `getConfigLocations()` or `getConfigPaths()`;
   * `classpath*:` locations and patterns (`spring/*-context.xml`), and telling apart files of the same name in
     different folders;
   * `${name}` and `${name:default}` looked up in the files a `property-placeholder`, `PropertyPlaceholderConfigurer`
     or `util:properties` names, and the default used where none sets it;
   * `<beans profile=...>` counting only for an active profile;
   * `jee:jndi-lookup` and `JndiObjectFactoryBean` as a point whose address is the JNDI name.
   Done when under-test's Spring XML guide for the external systems list no longer needs any of these.
4. **Settings loaded from a named file.** A `Properties` loaded from `orders.properties` and read with
   `getProperty("url")` should be looked up in that file, not in every settings file. A Spring profile's file
   (`application-mysql.properties`) is left out: read it for a test whose `@ActiveProfiles`, or
   `spring.profiles.active`, names that profile, and leave out a YAML document with
   `spring.config.activate.on-profile` the same way (today every document of a file counts, the first value winning).
5. **XML parsers that fetch a DTD.** A parser built without turning external entities off (`new SAXReader()`,
   `DocumentBuilderFactory`) can fetch a DTD over HTTP; there is no rule for it yet.
6. **Dispatch points.** Give how many places call each one, and put the few with most calls in the summary.
7. **Names traced in their scope.** A same-named variable anywhere in a type counts as the one traced, and `add`
   counts as an address setter on anything. Resolve the name to its declaration, and keep `add` to lists of
   addresses.
8. **Smaller faults from a review of the code**:
    * a point keeps one production caller's address: give every distinct one;
    * one `dns:` string anywhere in a class makes all its connections DNS ones: limit it to directory and JNDI calls;
    * Spring XML is matched by bare file name, and `classpath*:` locations are dropped without a note;
    * every module's resources are merged: read the module analysed;
    * a dotted literal not ending in a few known endings (`db.acme.de`) counts as a setting key;
    * a settings or XML file that fails to read is left out with no warning: warn, and list it in the output;
    * type variables are guessed from short capital names; any method named `setUp` counts as set-up; tests of an
      abstract class with no subclass are kept; symbol-solver failures outside calls aren't counted as not resolved.
9. **Tests still flagged in the field trials.** Commons Net's ListingFunctionalTest builds its hosts in a JUnit 3
    `suite()` from a `String[][]`, into each test's field through its constructor; jsoup's `postHtmlFile` posts a
    form whose address is relative to the page fetched; `SessionIT.multiThread`'s http reach; and a request never
    given a URL, which throws before it connects, should grade `none`.

## What it writes

10. **Grades in the facts, as external-calls/2.** The tool grades each reach (reaches, may reach, cannot reach, with
    the reason), as under-test's skill does today, and adds each reach's host, the file and line of the chain's first
    call, each test's framework, and the settings files and profiles a test that starts a framework loads. Bump the
    version whenever a value's meaning changes, and check the facts against under-test's schema in a test.

## How it is built

11. **Lists as resource files.** The lists of known types (address setters, Spring bean properties, stand-in
    servers, framework starters and runners, mocked types, open-database types) as files beside
    `connection-rules.txt`, with a rule kind checked against the schema's, and an example case for each library
    family the rules name (queues, JNDI, Kafka, JMS, Rabbit, Mongo, Redis, JPA, gRPC, AWS, OkHttp, JAX-RS, Netty).
12. **One way to ask the symbol solver.** About nine copies of its try and catch, three of walking a type's
    ancestors, each XML file parsed more than once, and dead code (`ConnectionPaths.toJson`).

## How it is run

13. **A classpath step as a tool.** A small script, for Windows and Unix, that picks the repo's wrapper (run with
    `sh` if it has lost its execute bit), else the Maven or Gradle the organisation names, and says which artifact
    and repository failed if one can't be downloaded.
14. **Newer and older Java.** The parser reads Java 21; set the level from the build, so Java 22 and later syntax
    parse, and so old code that names something `_` does.
15. **Large repos.** Each file is parsed twice (once to read, once by the type solver), and the solver's cache has no
    limit. Share the parse, and bound the cache.
16. **Test names.** Two overloads of a test method share one name in the facts.

## Checked against real repos

17. **More repos.** Openfire 4.6.0, spring-petclinic, jsoup 1.15.3 and Commons Net 3.6 are checked against their
    tests run under the airgap agent: every test that calls out is graded reaches. Check an old Spring XML
    application and a multi-module Maven build next, and turn each fault into a case first.

## Released

18. **The first release.** A Central Portal token, a signing key and the four secrets the release workflow reads,
    then the tag `v0.1.0`.
