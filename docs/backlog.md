# Backlog

What connection-paths doesn't do yet, most useful first. Each item says what it would change and how to tell it's
done. A behaviour that is built gets a case in the example repo, as [AGENTS.md](../AGENTS.md) says; a choice between
ways of building it gets a [decision record](decisions/README.md).

## What it finds

1. **A parameter's address, from the arguments its callers pass.** A library such as jsoup connects to a URL its
   caller passes, so every point's address is a `parameter`, and jsoup's tests, which connect to a server on
   localhost, can't be told local. Following a parameter back to each call site's argument (a bounded number of
   steps) would give hosts and mark those tests local. Needs the call sites kept with the graph, which costs memory
   on large repos. Done when jsoup's tests that use its local test server are local.
2. **Overloads joined through an interface.** A call through an interface matches implementations by name and number
   of parameters, so `read(String)` can reach an implementation's `read(URL)`. Compare simple parameter types, with a
   type variable matching any type.
3. **A `*` rule matched through a supertype that doesn't declare the method.** `JdbcTemplate#setDataSource` matches
   `JdbcOperations#*` because `JdbcTemplate` implements it. Match through a supertype only if it declares the method.
4. **Tests that start a framework.** `@SpringBootTest`, `@DataJpaTest`, `@WebMvcTest`, `@ContextConfiguration`,
   `@Testcontainers` and Quarkus or Micronaut tests reach connections through wiring the call graph can't see. Flag
   them, with the annotation, in a new field (a schema change in under-test).
5. **Clients that are only an annotated interface.** `@FeignClient`, Retrofit and Spring's `@HttpExchange` clients
   have no implementation in the source. A rule kind that matches calls on an interface carrying an annotation.
6. **A test inherited from an abstract base class.** It is reported once, under the base class, without the set-up of
   the classes that run it. Report it under each concrete subclass, with that subclass's set-up.
7. **Spring XML beans.** A `<bean class="...DataSource">` with a `url` property opens a connection the source never
   shows. Read bean definitions as connection points, with the property as the address.
8. **Settings, more precisely.** Spring's profiles (`application-test.yml` over `application.yml`), relaxed names
   (`SPRING_DATASOURCE_URL`), `@ConfigurationProperties` prefixes, and a `Properties` loaded from a named file.
   Today the first file in name order holds the value.
9. **Host look-ups of the machine's own addresses.** `InetAddress.getLocalHost()` and look-ups of a listen-address
   setting are noise in the host-look-up list; mark them local.
10. **Dispatch points.** Give how many places call each one, and put the few with most calls in the summary.

## How it is run

11. **A classpath step as a tool.** A small script, for Windows and Unix, that picks the repo's wrapper (run with
    `sh` if it has lost its execute bit), else the Maven or Gradle the organisation names; adds `-pl <module> -am
    -DskipTests compile` for a module with siblings; and says which artifact and repository failed if one can't be
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
