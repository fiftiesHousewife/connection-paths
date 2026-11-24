---
status: proposed
decision-makers: Maintainers
---

# It is published to Maven Central, with the runnable jar beside it, signed, with a bill of materials

**Decided by:** Maintainers · **Status:** proposed

## Context and Problem Statement

under-test's connection-list step runs under-test-external-calls on a repo, often inside a locked-down network that reaches the
internet only through the organisation's package repository, which mirrors Maven Central. A GitHub release can't be
reached from there. Organisations that let a tool in check where it came from and what it is built from.

## Considered Options

* **Maven Central, the same as under-test-airgap**: published through the Central Portal by nmcp, signed, with
  sources, Javadoc and a CycloneDX bill of materials; the runnable jar beside the library jar with the classifier
  `all`; the runnable jar and its bill of materials also on the GitHub release
* **GitHub releases only**: nothing to set up, but out of reach of most locked-down networks
* **GitHub Packages**: needs a token to read, even for a public package

## Decision Outcome

Proposed: Maven Central, as `io.github.fiftieshousewife.undertest:under-test-external-calls`, by the
[Release](../../.github/workflows/release.yml) workflow. The publishing set-up is the build-logic convention
`io.github.fiftieshousewife.publishing`, the same as under-test-airgap's.

| What | Where |
|---|---|
| The library jar, its POM, sources and Javadoc | `under-test-external-calls-<version>.jar` and beside it |
| The runnable jar, with its libraries in it | `under-test-external-calls-<version>-all.jar` |
| The bill of materials | `under-test-external-calls-<version>-cyclonedx.json` |
| Signatures | a `.asc` file beside each |

### Consequences

* The repository needs the secrets `CENTRAL_PORTAL_USERNAME`, `CENTRAL_PORTAL_PASSWORD` (a Central Portal user token
  for the `io.github.fiftieshousewife` namespace), `SIGNING_KEY` and `SIGNING_KEY_PASSWORD`.
* `./gradlew publishAllPublicationsToLocalRepository` writes what would be published to `build/local-repo`, unsigned
  without a key, to look at.
* under-test's connection-list step can fetch the jar by its coordinates.

## More Information

* Builds on: [Every change is checked on GitHub Actions, and a version tag publishes the jar](0002-every-change-is-checked-on-github-actions-and-a-version-tag-publishes-the-jar.md)
* under-test: [Connections and the test paths to them are found by under-test-external-calls, a JavaParser tool in its own repo](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0072-connections-and-the-test-paths-to-them-are-found-by-under-test-external-calls-a-javaparser-tool-in-its-own-repo.md)
