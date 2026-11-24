---
status: proposed
decision-makers: Maintainers
---

# Tests that start a framework are listed by their annotation, apart from the tests that reach a connection

**Decided by:** Maintainers · **Status:** proposed

## Context and Problem Statement

A test annotated `@SpringBootTest`, `@DataJpaTest`, `@Testcontainers`, `@QuarkusTest` or `@MicronautTest`, or run
with Spring's runner or extension, starts a framework that wires objects together and may open connections that no
call in the source shows. The call graph can't see them, so such a test was either missed or reached only through
whatever calls it makes itself.

## Considered Options

* **A list of its own**, `tests_starting_a_framework`: each test, the annotation, and the class it is on
* **A field on each entry in `tests`**: but `tests` holds only tests with a chain of calls to a connection, and a
  framework test may have none
* **Following the framework's wiring**: reading Spring's configuration, beans and auto-configuration is a tool of its
  own

## Decision Outcome

Proposed: a list of its own, with a count in the summary, in schema `connection-paths/3`. The annotation is looked
for on the test's class, its superclasses in the source, and the classes around a `@Nested` class. `tests` keeps its
meaning, and a test can be in both lists.

### Consequences

* A person sees which tests may connect through a framework, and by which annotation, without the tool following the
  framework.
* under-test's schema has to accept `connection-paths/3` before it can read the facts.
* An annotation of the project's own that carries `@SpringBootTest` (a meta-annotation) isn't recognised.

## More Information

* Builds on: [It reads source with JavaParser and takes the classpath from the build](0001-it-reads-source-with-javaparser-and-takes-the-classpath-from-the-build.md)
