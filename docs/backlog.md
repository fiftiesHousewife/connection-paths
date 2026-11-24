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
   `getProperty("url")` should be looked up in that file, not in every settings file. Spring profiles are read
   after the files without one; a test's own profile (`application-test.yml`) could give the value its tests use.
5. **XML parsers that fetch a DTD.** A parser built without turning external entities off (`new SAXReader()`,
   `DocumentBuilderFactory`) can fetch a DTD over HTTP; there is no rule for it yet.
6. **Dispatch points.** Give how many places call each one, and put the few with most calls in the summary.

## How it is run

7. **A classpath step as a tool.** A small script, for Windows and Unix, that picks the repo's wrapper (run with
    `sh` if it has lost its execute bit), else the Maven or Gradle the organisation names, and says which artifact
    and repository failed if one can't be downloaded.
8. **Newer and older Java.** The parser reads Java 21; set the level from the build, so Java 22 and later syntax
    parse, and so old code that names something `_` does.
9. **Large repos.** Each file is parsed twice (once to read, once by the type solver), and the solver's cache has no
    limit. Share the parse, and bound the cache.
10. **Test names.** Two overloads of a test method share one name in the facts.

## Checked against real repos

11. **More repos.** Openfire 4.6.0, spring-petclinic and jsoup 1.15.3 are checked against their tests run under
    the airgap agent: every test that calls out is graded reaches. Check an old Spring XML application and a
    multi-module Maven build next, and turn each fault into a case first.
