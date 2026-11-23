---
status: proposed
decision-makers: Maintainers
---

# Each connection's address is traced in the source and looked up in the repo's settings files

**Decided by:** Maintainers · **Status:** proposed

## Context and Problem Statement

A person planning a stand-in for a connection needs to know where it goes: a host, or the setting, environment
variable or JNDI name that holds the host. under-test's connection-list step asked a person to read the code for this.
Most of it a program can decide, which under-test's decision gives to connection-paths.

## Considered Options

* **Trace the address in the syntax tree**: from the connection call back through variables, fields, constructors,
  builders, the JDK's helpers and the source's own methods, to a literal or a name; then look a setting's key up in
  the repo's properties, YAML and XML files
* **Leave it to a person reading the code**: nothing to build, but slow, and done differently each time

## Decision Outcome

Proposed: trace it. For each connection point the facts give `address`:

| Field | What |
|---|---|
| `source` | `literal`, `setting`, `environment-variable`, `system-property`, `jndi-name`, `classpath-resource`, `local-file`, `dns`, `parameter` or `built-at-run-time` |
| `value` | The literal (with `{?}` for each part that can't be known), the setting's, variable's or JNDI name, the parameter, or the code that builds it |
| `via` | For a setting: the method or annotation that reads it (`JiveGlobals.getProperty`, `@Value`) |
| `setting_value`, `setting_file`, `setting_line` | For a setting: its value in the repo's settings files, and where |
| `placeholder` | The value looks like one a person fills in: `YOUR-...`, `<...>`, `${...}`, `CHANGE-ME`, `example.com` |

How it traces:

* The call's first argument, if it looks like an address or is a name to follow; otherwise what the call is made on
  (`dataSource.getConnection()`). For a process, the command.
* A variable or field: its initialiser, its assignments (not `+=`, which adds to a value), and address setters such as
  `setUrl`; a field of the class or of a class around it; Spring's `@Value("${key}")`.
* A call into the source: what the method returns. A builder chain or one of the JDK's helpers (`URI.resolve`,
  `String.format`): what it is called on, then its first argument. A factory's argument (`getInstance`, `forName`)
  is never an address.
* A call whose argument is a dotted key (`crowd.server.url`) reads a setting; `System.getenv` and
  `System.getProperty` read a variable or property; `lookup` a JNDI name; `getResource` a classpath resource.
* Where a name is given several values, the one that says most is kept: a whole literal, then a setting, then a
  JNDI name or variable, then a local resource, then a partial literal, then a parameter.
* Settings files are every `.properties`, `.yml`, `.yaml` and `.xml` file under the `--module` folder and any
  `--settings` folder, outside build output; XML is keyed by element path, with and without the root element.

### Consequences

* The facts' schema in under-test gains `address`; the connection-list skill reads it instead of the code.
* A trace stops after 40 steps, and says `built-at-run-time` with the code it reached.

## More Information

* Builds on: [It reads source with JavaParser and takes the classpath from the build](0001-it-reads-source-with-javaparser-and-takes-the-classpath-from-the-build.md)
* under-test: [connection-paths does everything a program can decide, and the connection-list skill only what needs judgement](https://github.com/fiftiesHousewife/under-test/blob/main/docs/decisions/0074-connection-paths-does-everything-a-program-can-decide-and-the-connection-list-skill-only-what-needs-judgement.md)
