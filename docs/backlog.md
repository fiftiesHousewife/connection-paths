# Backlog

What under-test-external-calls doesn't do yet, most useful first. Each item says what it would change and how to tell it's
done. A behaviour that is built gets a case in the example repo, as [AGENTS.md](../AGENTS.md) says; a choice between
ways of building it gets a [decision record](decisions/README.md).

## What it finds

1. **A Spring Data repository's address is its data source.** A call on a repository is a database point, but its
   address comes out as the call's argument or receiver, so each calling class is a system of its own and some are
   marked local and some not. Its address is `spring.datasource.url` if it is set; otherwise Spring Boot's embedded
   database when H2, HSQLDB or Derby is on the classpath, which is local. Done when spring-petclinic's repositories
   are one local database.
2. **An embedded server started in a test's set-up is a stand-in.** A test class that starts a server on this
   machine from `@BeforeAll`, directly or through a helper of the tests (jsoup's `TestServer.start()` starting
   Jetty), should list it in `stand_ins`, as a rule or extension that starts one is listed now. So should
   Testcontainers' `@Container` fields. Done when jsoup's `ConnectTest` and spring-petclinic's
   `MySqlIntegrationTests` list theirs.
3. **A relative path in a test of the app on a port is local.** In a `@SpringBootTest(webEnvironment =
   RANDOM_PORT)` or `DEFINED_PORT` test, `TestRestTemplate` and `RestTemplate` calls with a path such as
   `/owners/1` go to the app under test on localhost; their `address_for_tests` should be `local`, not `run-time`.
4. **Spring Boot's Docker Compose support** (`spring.docker.compose.*` in a test's settings) runs the `docker
   compose` program, which no point records.
5. **An address held in an object's field.** jsoup's `Jsoup.connect(url)` puts the URL in a `Request`, and
   `execute(req)` later reads it back out, so its points' address is jsoup's stand-in `http://undefined/` and its
   tests, which connect to a server on localhost, can't be told local. Following a value through a setter into a
   field and out through a getter, across calls, and giving each test the address its own chain passes, would. At
   least, a class's own placeholder such as `http://undefined/` should not be taken as an address. For a library,
   a system named after its connecting class says little: the hosts are its callers'. Done when jsoup's tests that
   name a host in `Jsoup.connect` say that host.
6. **A parameter's address, for the connection point itself.** Each test's reach follows a parameter back to the
   argument its caller passes (`address_for_tests`), but the point's own `address` still says `parameter`: give
   it the addresses all its callers pass.
7. **Tests that load a Spring XML context.** A bean in Spring XML is a connection point, but no test reaches it: a
   test whose `@ContextConfiguration` names the file, or that builds a `ClassPathXmlApplicationContext` on it,
   could reach each of its beans.
8. **Settings loaded from a named file.** A `Properties` loaded from `orders.properties` and read with
   `getProperty("url")` should be looked up in that file, not in every settings file. Spring profiles are read
   after the files without one; a test's own profile (`application-test.yml`) could give the value its tests use.
9. **XML parsers that fetch a DTD.** A parser built without turning external entities off (`new SAXReader()`,
   `DocumentBuilderFactory`) can fetch a DTD over HTTP; there is no rule for it yet.
10. **Dispatch points.** Give how many places call each one, and put the few with most calls in the summary.

## How it is run

11. **A classpath step as a tool.** A small script, for Windows and Unix, that picks the repo's wrapper (run with
    `sh` if it has lost its execute bit), else the Maven or Gradle the organisation names, and says which artifact and repository failed if one can't be
    downloaded.
12. **Newer and older Java.** The parser reads Java 21; set the level from the build, so Java 22 and later syntax
    parse, and so old code that names something `_` does.
13. **Large repos.** Each file is parsed twice (once to read, once by the type solver), and the solver's cache has no
    limit. Share the parse, and bound the cache.
14. **Test names.** Two overloads of a test method share one name in the facts.

## Checked against real repos

15. **More repos.** Openfire 4.6.0 is the one checked by a person so far; jsoup 1.15.3 has been run without a
    classpath. Check a Spring Boot application (Spring Petclinic, with its classpath), an old Spring XML application
    and a multi-module Maven build, and turn each fault into a case first.
