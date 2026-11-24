---
status: proposed
decision-makers: Maintainers
---

# Connection points are grouped into systems, and local connections are part of none

**Decided by:** Maintainers · **Status:** proposed

## Context and Problem Statement

A person plans one stand-in per system a test reaches, not per connection point: on Openfire, 54 points go to about a
dozen systems, and every test that reaches the database lists both of two database providers that are really
alternatives. Some connections never leave the machine: a database in memory or in a file, a classpath resource,
localhost. They need no stand-in.

## Considered Options

* **Group in under-test-external-calls, by what the address shows**
* **Leave grouping to the connection-list skill**: a person's judgement each time, for something a rule decides

## Decision Outcome

Proposed: group in under-test-external-calls.

* **Local, part of no system** (`local` on the point, saying why): an HSQLDB, H2 or Derby database in memory or in a
  file, SQLite, a `file:`, `jar:` or `classpath:` address, localhost, a classpath resource, a class's own jar, a
  path given to a URL, and an address that is a parameter no code in the source passes.
* **Not a system either**: host look-ups and processes, which the safety inventory covers.
* **Not local**: a look-up on the machine's DNS servers, such as JNDI's DNS provider finding a domain's service
  records. The servers are other machines, and a test that looks a remote domain up reaches them.
* **A system** is the points that go to one host; failing that, to one setting, variable or JNDI name; failing that,
  those opened in one class. A point with no address of its own joins the system of another point in the same
  method, then the same class.
* **Alternatives**: systems whose points override the same interface or superclass method in the source, so a
  program uses one of them, not all.
* **Each system** gives the tests that may reach it, and its `shared_chain`: the calls all those tests go through,
  where one stand-in could serve them all. Each test lists the systems it may reach.

### Consequences

* The facts' schema in under-test gains `systems`, each test's `systems`, and each point's `local`.
* The connection-list skill proposes a stand-in per system and asks a person only about what is left.

## More Information

* Builds on: [Each connection's address is traced in the source and looked up in the repo's settings files](0006-each-connections-address-is-traced-in-the-source-and-looked-up-in-the-repos-settings-files.md)
