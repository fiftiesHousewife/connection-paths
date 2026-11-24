---
name: add-a-connection-rule
description: Add a call that opens or uses a connection to under-test-external-calls' built-in rules (src/main/resources/connection-rules.txt), with a case that shows it. Use when a library's client (a database driver, an HTTP, mail, queue, directory or cloud client) is missing from the rules.
---

# Adding a connection rule

The rules are in [connection-rules.txt](../../../src/main/resources/connection-rules.txt), one a line:
`<kind> <type>#<method>`. A method of `*` means every method but the constructor; `<init>` means the constructor. A
rule matches the type and anything that extends or implements it, so name the **interface** a client implements
(`JdbcOperations`, not `JdbcTemplate`) and one rule covers every implementation. A client that is only an
interface, made at run time from its annotations (Feign, Retrofit, Spring's `@HttpExchange`), takes a rule
`<kind> @<annotation>`: it matches calls to methods carrying the annotation, or declared in a type that does.

1. **Pick the kind** from those already used: `database`, `http`, `mail`, `directory`, `jndi`, `queue`, `socket`,
   `url`, `host-lookup`, `process`. A new kind changes the facts file's schema, which lives in
   [under-test](https://github.com/fiftiesHousewife/under-test/blob/main/schema/external-calls.schema.json): change
   it there first.
2. **Pick the call that connects**, not every call on the client: building a request or a client object usually
   doesn't connect, sending or executing does. Use `*` only for a type whose every method talks to the outside
   (`JdbcOperations`, `EntityManager`).
3. **Add the line** in the kind's block, keeping the blocks in the order they are.
4. **Add a case** with the add-an-example-case skill when the rule needs matching no rule has needed before (a
   constructor, a supertype the example repo doesn't have). A rule matched the same way as one already there needs no
   new case.
5. Run `./gradlew check`.

A rule that only one organisation needs goes in its own file, passed with `--rules`, not here.
