# Backlog

What under-test-external-calls doesn't do yet, most useful first. Each item says what it would change and how to
tell it's done. A behaviour that is built gets a case in the example repo, as [AGENTS.md](../AGENTS.md) says; a
choice between ways of building it gets a [decision record](decisions/README.md).

## What it finds

1. **A server's instance only a running application has.** `XMPPServer.getInstance()` is null in a test that
   starts no server, so a chain through it doesn't run.
2. **Spring XML read as Spring reads it.** A test or code that loads a context now reaches the beans in the file it
   names, matched by its path on the classpath. Still to do, each a rule a program can follow:
   * a test that starts Spring through a JUnit 3 base class or an annotation of the repo's own, listed among those
     that start a framework;
   * `${name}` looked up in the files a `property-placeholder`, `PropertyPlaceholderConfigurer` or
     `util:properties` names, the test classpath's copy first, rather than in every settings file (it differs only
     where a key is set in more than one);
   Done when under-test's Spring XML guide for the external systems list no longer needs any of these.
3. **Settings loaded from a named file.** A `Properties` loaded from `orders.properties` and read with
   `getProperty("url")` should be looked up in that file, not in every settings file. A test's profiles now come
   from `@ActiveProfiles`; still to do: `spring.profiles.active` set in a settings file, a YAML document with
   `spring.config.activate.on-profile` left out but for its profile (today every document of a file counts, the
   first value winning), and a test resource that shadows the main one of the same path, as only one is on the
   classpath (today both count, the test one first).
4. **XML parsers that fetch a DTD.** A parser built without turning external entities off (`new SAXReader()`,
   `DocumentBuilderFactory`) can fetch a DTD over HTTP; there is no rule for it yet.
5. **Dispatch points.** Give how many places call each one, and put the few with most calls in the summary.
6. **`add` as an address setter on anything.** It is one so a list of hosts built with `hosts.add(...)` is followed;
   keep it to collections of strings once a real repo shows it matching something else.
7. **Smaller faults from a review of the code**:
    * a class made only in a program's `main` counts for a test only if no chain goes through that `main`;
    * a dotted literal not ending in a few known endings (`db.acme.de`) given to a method outside the JDK counts as a
      setting key;
    * a settings or XML file that fails to read is warned about, but not listed in the output;
    * type variables are guessed from short capital names; symbol-solver failures outside calls aren't counted as not
      resolved.
8. **Tests still flagged in the field trials.** Commons Net's ListingFunctionalTest builds its hosts in a JUnit 3
    `suite()` from a `String[][]`, into each test's field through its constructor; jsoup's `postHtmlFile` posts a
    form whose address is relative to the page fetched; `SessionIT.multiThread`'s http reach; and jsonld-java's tests
    that read a JSON-LD document from a test resource, which may name a remote `@context` (reading it out of the
    resource the chain opens would tell).

## What it writes

9. **What external-calls/2 leaves out.** A test's `profiles` from `spring.profiles.active` and a YAML file's
    on-profile documents; a `client_field` declared in a superclass of the test class; a `first_call` for a chain that starts in
    the test class's own field initialisers.

## How it is built

10. **Lists as resource files.** The lists of known types (address setters, Spring bean properties, stand-in
    servers, framework starters and runners, mocked types, open-database types) as files beside
    `connection-rules.txt`, with a rule kind checked against the schema's, and an example case for each library
    family the rules name (queues, JNDI, Kafka, JMS, Rabbit, Mongo, Redis, JPA, gRPC, AWS, OkHttp, JAX-RS, Netty).
11. **One way to ask the symbol solver.** About nine copies of its try and catch, three of walking a type's
    ancestors, each XML file parsed more than once, and dead code (`ConnectionPaths.toJson`).

## How it is run

12. **A classpath step as a tool.** A small script, for Windows and Unix, that picks the repo's wrapper (run with
    `sh` if it has lost its execute bit), else the Maven or Gradle the organisation names, and says which artifact
    and repository failed if one can't be downloaded.
13. **Newer and older Java.** The parser reads Java 21; set the level from the build, so Java 22 and later syntax
    parse, and so old code that names something `_` does.
14. **Large repos.** Each file is parsed twice (once to read, once by the type solver), and the solver's cache has no
    limit. Share the parse, and bound the cache.
15. **Test names.** Two overloads of a test method share one name in the facts.

## Checked against real repos

16. **More repos.** Openfire 4.6.0, spring-petclinic, jsoup 1.15.3, Commons Net 3.6 and jsonld-java 0.12.0 are
    checked against their tests run under the airgap agent: no test that calls out is graded cannot-reach. Check an old Spring XML
    application and a multi-module Maven build next, and turn each fault into a case first.

## Released

17. **The first release.** A Central Portal token, a signing key and the four secrets the release workflow reads,
    then the tag `v0.1.0`.
