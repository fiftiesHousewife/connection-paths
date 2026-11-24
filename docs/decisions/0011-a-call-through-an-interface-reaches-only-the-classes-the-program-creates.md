---
status: proposed
decision-makers: Maintainers
---

# A call through an interface reaches only the classes the program creates

**Decided by:** Maintainers · **Status:** proposed

## Context and Problem Statement

A call through an interface or superclass was followed to every method in the source that overrides it. On Openfire,
217 tests were left reaching JNDI through `ConnectionProvider.start()`, dispatched to `JNDIDataSourceProvider`, a
class the source never creates: it is made only by name, from a setting no test file gives. A class nothing creates
can't have its methods called.

## Considered Options

* **Follow only into created classes** (rapid type analysis): an override is followed if its class, or a subclass of
  it, is created somewhere in the code read
* **Mark the reach as through a class made only by name**, and keep it: a schema change, for a case a person can't
  act on differently
* **Keep following every override**, as before

## Decision Outcome

Proposed: follow only into created classes. A class counts as created if the source makes one with `new`, `X::new`
or an anonymous subclass; if it is an enum, a test class, or carries an annotation by which a framework creates it
(`@Component`, `@Service`, `@Named`, `@ApplicationScoped`, `@WebServlet` and the like); or if it is named, and so may
be created by name, in a string in the source, a settings value, a Spring XML bean or a `META-INF/services` file.

### Consequences

* On Openfire, no test reaches JNDI. Fewer overrides also leave fewer dispatch points over the limit (338 to 142),
  so some calls are now followed that weren't, and their reaches are listed.
* A class created only by a library, from a name built at run time, isn't reached. That is the case this decision
  is for; a class a framework creates by an annotation not listed is missed too, and its annotation should be added.

## More Information

* Builds on: [It reads source with JavaParser and takes the classpath from the build](0001-it-reads-source-with-javaparser-and-takes-the-classpath-from-the-build.md)
