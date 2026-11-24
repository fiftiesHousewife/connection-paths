# Backlog

What connection-paths doesn't do yet, most useful first. Each item says what it would change and how to tell it's
done. A behaviour that is built gets a case in the example repo, as [AGENTS.md](../AGENTS.md) says; a choice between
ways of building it gets a [decision record](decisions/README.md).

## What it finds

1. **An address held in an object's field.** jsoup's `Jsoup.connect(url)` puts the URL in a `Request`, and
   `execute(req)` later reads it back out, so its points' address is jsoup's stand-in `http://undefined/` and its
   tests, which connect to a server on localhost, can't be told local. Following a value through a setter into a
   field and out through a getter, across calls, and giving each test the address its own chain passes, would. Done
   when jsoup's tests that use its local test server are local.
2. **A parameter's address, from the arguments its callers pass.** Where a point's address is a parameter
   (`DocumentReader#read(URL)`), follow each test's chain back to the argument passed, a bounded number of steps,
   and give the address on that test's reach.
3. **A test inherited from an abstract base class.** It is reported once, under the base class, without the set-up of
   the classes that run it. Report it under each concrete subclass, with that subclass's set-up.
4. **Spring XML beans.** A `<bean class="...DataSource">` with a `url` property opens a connection the source never
   shows. Read bean definitions as connection points, with the property as the address.
5. **Settings, more precisely.** Spring's profiles (`application-test.yml` over `application.yml`), relaxed names
   (`SPRING_DATASOURCE_URL`), `@ConfigurationProperties` prefixes, and a `Properties` loaded from a named file.
   Today the first file in name order holds the value.
6. **Host look-ups of the machine's own addresses.** `InetAddress.getLocalHost()` and look-ups of a listen-address
   setting are noise in the host-look-up list; mark them local.
7. **Dispatch points.** Give how many places call each one, and put the few with most calls in the summary.

## How it is run

8. **A classpath step as a tool.** A small script, for Windows and Unix, that picks the repo's wrapper (run with
    `sh` if it has lost its execute bit), else the Maven or Gradle the organisation names; adds `-pl <module> -am
    -DskipTests compile` for a module with siblings; and says which artifact and repository failed if one can't be
    downloaded.
9. **Newer and older Java.** The parser reads Java 21; set the level from the build, so Java 22 and later syntax
    parse, and so old code that names something `_` does.
10. **Large repos.** Each file is parsed twice (once to read, once by the type solver), and the solver's cache has no
    limit. Share the parse, and bound the cache.
11. **Test names.** Two overloads of a test method share one name in the facts.

## Checked against real repos

12. **More repos.** Openfire 4.6.0 is the one checked by a person so far; jsoup 1.15.3 has been run without a
    classpath. Check a Spring Boot application (Spring Petclinic, with its classpath), an old Spring XML application
    and a multi-module Maven build, and turn each fault into a case first.
