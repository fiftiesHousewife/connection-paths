---
status: proposed
decision-makers: Maintainers
---

# A sibling module's class folder on the classpath is read from its source

**Decided by:** Maintainers · **Status:** proposed

## Context and Problem Statement

The classpath of a module in a multi-module build, from `-pl <module> -am ... dependency:build-classpath`, names its
sibling modules by their class folders (`i18n/target/classes`). JavaParser can't read class folders, so calls into a
sibling were unresolved, and a connection opened in a sibling was missed.

## Considered Options

* **Read the sibling's source instead**, from the usual place next to its class folder
* **Read class folders as compiled classes**: JavaParser has no solver for them, and a test's path through a sibling
  needs its source anyway

## Decision Outcome

Proposed: a class folder at `<module>/target/classes` or `<module>/build/classes/java/main` whose module has
`src/main/java` is read as production source, as if given with `--main`, and the classpath notes say so. Any other
class folder is still skipped with a note.

### Consequences

* Connection points and paths in sibling modules are found without listing each sibling with `--main`.

## More Information

* Builds on: [It reads source with JavaParser and takes the classpath from the build](0001-it-reads-source-with-javaparser-and-takes-the-classpath-from-the-build.md)
