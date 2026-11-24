# Agent guide

Guidance for any AI coding assistant working in this repo, and for the people directing it.

**What this repo is:** under-test-external-calls, a command-line tool that finds where Java code opens external connections and
which tests reach them, from source. It is a tool of [under-test](https://github.com/fiftiesHousewife/under-test),
whose plain-English rules ([GLOSSARY.md](https://github.com/fiftiesHousewife/under-test/blob/main/GLOSSARY.md)) it
follows: no repurposed words, no dates in text, things referred to by name.

## Where things are
| Path | What |
|---|---|
| `src/main/java/.../connectionpaths/` | `Main`, `OptionsParser` and `Options` are the command line; `ConnectionPaths` runs the rest |
| `.../source/` | `SourceReader` reads the source into a `SourceModel`, with its `TypeHierarchy` and `TestModel`; `CallResolver` resolves each call; `TypeSolvers` sets up JavaParser's symbol solver from the classpath |
| `.../rules/` | `ConnectionRules` reads the rules; `AnnotatedClients` finds clients that are only an annotated interface |
| `.../address/` | `AddressTracer` traces each connection's address back through the source; `Locality` says which stay on this machine |
| `.../settings/` | `SettingsFiles` reads the repo's properties, YAML and XML settings; `XmlFiles` walks an XML file safely |
| `.../spring/` | `SpringXml` and `SpringBeans` find the beans defined in Spring XML |
| `.../graph/` | `CallGraph` walks back from connections; `TestChains` gives each test's chain |
| `.../facts/` | `FactsBuilder` builds the output, `Facts`; `SystemsBuilder` groups points into systems |
| `src/main/resources/connection-rules.txt` | The calls that count as opening or using a connection |
| `src/test/resources/example/` | An example repo with known answers, some listed in `src/test/resources/expected/`; the `Example*Test` classes check each one |
| `tools/print-classpath.gradle` | Writes a Gradle build's classpath for `--classpath` |
| [`docs/decisions/`](docs/decisions/README.md) | This repo's decisions, in MADR form |
| [`docs/backlog.md`](docs/backlog.md) | What it doesn't do yet, most useful first |
| [`docs/evaluations/`](docs/evaluations/) | Comparisons with other tools that decisions rest on, with what's needed to run them again |
| `build-logic/`, `config/quality/` | The Gradle conventions and quality rules, from which under-test's shared java-conventions plugin is seeded; `io.github.fiftieshousewife.publishing` is how it is published |
| [`.github/workflows/`](.github/workflows/) | Build: `./gradlew check` on every pull request, and the jar run on Java 17. Release: a tag such as `v1.2.3` publishes to Maven Central and GitHub |
| [`.github/skills/`](.github/skills/) | This repo's AI assistant skills (below) |

## Code standards (enforced by `./gradlew check`)
Each of these fails the build:
- **Size:** methods of 20 lines or fewer, not counting blank ones; files of 150 lines or fewer; lines of 120 characters
  or fewer.
- **Simplicity:** cyclomatic complexity of 6 or less and cognitive complexity of 7 or less for each method; boolean
  expressions of at most three parts; no copy-pasted code.
- **Parameters:** no more than three for a method or constructor (use a Lombok `@Builder`), and no more than three
  components in a record.
- **Packages** by area, each with a public entry point and package-private helpers.
- **Comments:** no comment on a field (name it so it needs none, or say it on the class or method); Javadoc only on a
  public type and its public members, and `//` comments elsewhere.
- **Tests:** at least 80% line coverage; each test makes one assertion, or one `assertAll` holding several.
- **Code:** logging through Lombok `@Slf4j`, never `System.out`; no forbidden JDK APIs; SpotBugs with FindSecBugs
  clean; final locals, braces always, no star imports.

And one that is reported, by the `pmdAdvice` task, without failing the build: prefer streams to for-each loops. It is
built with Java 25 and runs on Java 17 (`--release 17`).

## Skills
| Skill | Use it to |
|---|---|
| [add-an-example-case](.github/skills/add-an-example-case/SKILL.md) | Turn a fault found on a real repo, or a new behaviour, into a case in the example repo, then fix it |
| [add-a-connection-rule](.github/skills/add-a-connection-rule/SKILL.md) | Add a call that opens or uses a connection to the rules |
| [write-a-decision-record](.github/skills/write-a-decision-record/SKILL.md) | Draft a decision record in `docs/decisions/` |
| grilling (installed by APM) | Interview a person about a decision. In Claude Code, ask one question at a time with the AskUserQuestion tool |
| architecture-decision-records (installed by APM) | The mechanics of writing a MADR record; use it with write-a-decision-record |

Outside skills are declared in [`apm.yml`](apm.yml), pinned, and installed with `apm install`. Run it once after
cloning. Claude Code finds this repo's own skills through links in `.claude/skills/`.

## Changing what it finds
Every behaviour has a case in the example repo. A fault found on a real repo gets a small case of its own there before
it is fixed, so it stays fixed (under-test's decision on checking tools against real public repos).

## Before you finish
Run `./gradlew check`. It must pass.
