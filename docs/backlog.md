# Backlog

What under-test-external-calls doesn't do yet, most useful first. Each item says what it would change and how to tell it's
done. A behaviour that is built gets a case in the example repo, as [AGENTS.md](../AGENTS.md) says; a choice between
ways of building it gets a [decision record](decisions/README.md).

## What it finds

1. **An address held in an object's field, for the point itself.** A test's own call now gives its reach the
   address it hands a builder (`Jsoup.connect(url)`), but the point's `address` still says the parameter or the
   class's placeholder: following a value through a setter into a field and out through a getter, across calls,
   would give the point the addresses its callers set. For a library, a system named after its connecting class says
   little: the hosts are its callers'.
2. **A server's instance only a running application has.** `XMPPServer.getInstance()` is null in a test that
   starts no server, so a chain through it doesn't run (Openfire's GroupJIDTest).
3. **A parameter's address, for the connection point itself.** Each test's reach follows a parameter back to the
   argument its caller passes (`address_for_tests`), but the point's own `address` still says `parameter`: give
   it the addresses all its callers pass.
4. **Tests that load a Spring XML context.** A bean in Spring XML is a connection point, but no test reaches it: a
   test whose `@ContextConfiguration` names the file, or that builds a `ClassPathXmlApplicationContext` on it,
   could reach each of its beans.
5. **Settings loaded from a named file.** A `Properties` loaded from `orders.properties` and read with
   `getProperty("url")` should be looked up in that file, not in every settings file. Spring profiles are read
   after the files without one; a test's own profile (`application-test.yml`) could give the value its tests use.
6. **XML parsers that fetch a DTD.** A parser built without turning external entities off (`new SAXReader()`,
   `DocumentBuilderFactory`) can fetch a DTD over HTTP; there is no rule for it yet.
7. **Dispatch points.** Give how many places call each one, and put the few with most calls in the summary.

## How it is run

8. **A classpath step as a tool.** A small script, for Windows and Unix, that picks the repo's wrapper (run with
    `sh` if it has lost its execute bit), else the Maven or Gradle the organisation names, and says which artifact and repository failed if one can't be
    downloaded.
9. **Newer and older Java.** The parser reads Java 21; set the level from the build, so Java 22 and later syntax
    parse, and so old code that names something `_` does.
10. **Large repos.** Each file is parsed twice (once to read, once by the type solver), and the solver's cache has no
    limit. Share the parse, and bound the cache.
11. **Test names.** Two overloads of a test method share one name in the facts.

## Checked against real repos

12. **More repos.** Openfire 4.6.0, spring-petclinic and jsoup 1.15.3 are checked against their tests run under
    the airgap agent: every test that calls out is graded reaches. Check an old Spring XML application and a
    multi-module Maven build next, and turn each fault into a case first.
