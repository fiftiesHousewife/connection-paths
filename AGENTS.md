# Agent guide

Guidance for any AI coding assistant working in this repo, and for the people directing it.

**What this repo is:** connection-paths, a command-line tool that finds where Java code opens external connections and
which tests reach them, from source. It is a tool of [under-test](https://github.com/fiftiesHousewife/under-test),
whose plain-English rules ([GLOSSARY.md](https://github.com/fiftiesHousewife/under-test/blob/main/GLOSSARY.md)) it
follows: no repurposed words, no dates in text, things referred to by name.

## Where things are
| Path | What |
|---|---|
| `src/main/java/.../connectionpaths/` | `SourceReader` reads the source; `CallResolver` resolves each call; `CallGraph` walks back from connections; `AddressTracer` and `NameTracer` trace each address; `SettingsFiles` reads properties, YAML and XML; `Locality` and `SystemsBuilder` sort points into local ones and systems; `Frameworks` knows the annotations that start a framework, `AnnotatedClients` the clients that are only an annotated interface, and `SpringXml` and `SpringBeans` the beans defined in Spring XML; `FactsBuilder` and `Facts` are the output |
| `src/main/resources/connection-rules.txt` | The calls that count as opening or using a connection |
| `src/test/resources/example/` | An example repo with known answers; `ExampleRepoTest` checks each one |
| `tools/print-classpath.gradle` | Writes a Gradle build's classpath for `--classpath` |
| [`docs/decisions/`](docs/decisions/README.md) | This repo's decisions, in MADR form |
| [`docs/backlog.md`](docs/backlog.md) | What it doesn't do yet, most useful first |
| [`docs/evaluations/`](docs/evaluations/) | Comparisons with other tools that decisions rest on, with what's needed to run them again |
| `build-logic/`, `config/quality/` | The same Gradle conventions and quality rules as under-test; `io.github.fiftieshousewife.publishing` is how it is published |
| [`.github/workflows/`](.github/workflows/) | Build: `./gradlew check` on every pull request, and the jar run on Java 17. Release: a tag such as `v1.2.3` publishes to Maven Central and GitHub |
| [`.github/skills/`](.github/skills/) | This repo's AI assistant skills (below) |

## Code standards (enforced by `./gradlew check`)
The same as under-test's: at least 80% line coverage; no more than three parameters (use a Lombok `@Builder`); no record
with more than three components; a test with more than one assertion wraps them in `assertAll`; logging through Lombok
`@Slf4j`, never `System.out`; no forbidden JDK APIs; short, simple methods; no copy-pasted code; SpotBugs with
FindSecBugs clean; final locals, braces always, no star imports; lines of 120 characters or fewer. It is built with
Java 25 and runs on Java 17 (`--release 17`).

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
