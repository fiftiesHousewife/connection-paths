# Decisions

This repo's decisions, made by its maintainers, in [MADR](https://adr.github.io/madr/) form, one small decision per
record. Start a new one from [the template](template.md), or with the
[write-a-decision-record](../../.github/skills/write-a-decision-record/SKILL.md) skill. Decisions about the toolkit as
a whole are in [under-test](https://github.com/fiftiesHousewife/under-test/tree/main/docs/decisions).

| Decision | Status |
|---|---|
| [It reads source with JavaParser and takes the classpath from the build](0001-it-reads-source-with-javaparser-and-takes-the-classpath-from-the-build.md) | accepted |
| [Every change is checked on GitHub Actions, and a version tag publishes the jar](0002-every-change-is-checked-on-github-actions-and-a-version-tag-publishes-the-jar.md) | proposed |
| [AI assistant skills live in .github/skills, and outside ones come through APM, pinned](0003-ai-assistant-skills-live-in-github-skills-and-outside-ones-come-through-apm-pinned.md) | proposed |
| [It is published to Maven Central, with the runnable jar beside it, signed, with a bill of materials](0004-it-is-published-to-maven-central-with-the-runnable-jar-beside-it-signed-with-a-bill-of-materials.md) | proposed |
| [connection-paths is built, not adopted, after checking the existing tools](0005-connection-paths-is-built-not-adopted-after-checking-the-existing-tools.md) | accepted |
| [Each connection's address is traced in the source and looked up in the repo's settings files](0006-each-connections-address-is-traced-in-the-source-and-looked-up-in-the-repos-settings-files.md) | proposed |
| [Connection points are grouped into systems, and local connections are part of none](0007-connection-points-are-grouped-into-systems-and-local-connections-are-part-of-none.md) | proposed |
| [A sibling module's class folder on the classpath is read from its source](0008-a-sibling-modules-class-folder-on-the-classpath-is-read-from-its-source.md) | rejected |
| [Tests that start a framework are listed by their annotation, apart from the tests that reach a connection](0009-tests-that-start-a-framework-are-listed-by-their-annotation-apart-from-the-tests-that-reach-a-connection.md) | proposed |
| [Each reach says whether it runs a mock, and where the test's own settings send it](0010-each-reach-says-whether-it-runs-a-mock-and-where-the-tests-own-settings-send-it.md) | proposed |
