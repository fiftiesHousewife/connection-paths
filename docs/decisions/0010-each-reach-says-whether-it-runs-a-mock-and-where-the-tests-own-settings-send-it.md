---
status: proposed
decision-makers: Maintainers
---

# Each reach says whether it runs a mock, and where the test's own settings send it

**Decided by:** Maintainers · **Status:** proposed

## Context and Problem Statement

On Openfire, none of the 230 tests listed as reaching a connection could reach a real external system with the
repo's own test settings. Many chains pass through objects the test replaces with mocks, so the real code never
runs; the database's address comes from a setting that no file on the test classpath gives; and some tests start an
embedded server on this machine for the system they use. A person sorting the tests needs to tell these apart.

## Considered Options

* **Mark each reach**, keeping the chain: the step that would run a mock, and how the test's settings resolve the
  address
* **Drop the reaches** that run a mock or have no address for tests: simpler output, but a person can't check why a
  test was left out, and a mock set up only in some tests' set-up would hide real reaches

## Decision Outcome

Proposed: mark each reach, in schema `connection-paths/4`:

* `through_mock`: the first step after the test that is an instance method of a type the test mocks: a `@Mock` or
  `@MockBean` field, or `mock(T.class)` in the test, what runs with it, or test-source methods they call (three calls
  deep). A mocked supertype counts; a static method or a constructor runs its own code.
* `address_for_tests`: the point's address resolved from the settings on the test classpath, test resources before
  main ones: `host`, `local`, `none` (a setting none of them gives, or gives only a placeholder for) or `run-time`.
* `stand_ins`, on each test: the classes of its `@Rule`, `@ClassRule` or `@RegisterExtension` fields that start a
  server on this machine, such as an embedded LDAP, mail, HTTP or database server.

### Consequences

* A test whose every reach runs a mock, has no address for tests, or has a stand-in can be treated without a stand-in
  system of its own.
* Mocks are known per test class and its helpers, not per test, so a mock made in one test marks its class's other
  tests too.
* A Spring profile a test turns on (`@ActiveProfiles`) isn't read yet: its file ranks as any other test resource.
* under-test's schema has to accept `connection-paths/4`.

## More Information

* Builds on: [Each connection's address is traced in the source and looked up in the repo's settings files](0006-each-connections-address-is-traced-in-the-source-and-looked-up-in-the-repos-settings-files.md)
