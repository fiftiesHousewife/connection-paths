# Agent guide

Guidance for any AI coding assistant working in this repo, and for the people directing it.

**What this repo is:** connection-paths, a command-line tool that finds where Java code opens external connections and
which tests reach them, from source. It is a tool of [under-test](https://github.com/fiftiesHousewife/under-test),
whose plain-English rules ([GLOSSARY.md](https://github.com/fiftiesHousewife/under-test/blob/main/GLOSSARY.md)) it
follows: no repurposed words, no dates in text, things referred to by name.

## Where things are
| Path | What |
|---|---|
| `src/main/java/.../connectionpaths/` | `SourceReader` reads the source; `CallResolver` resolves each call; `CallGraph` walks back from connections; `FactsBuilder` and `Facts` are the output |
| `src/main/resources/connection-rules.txt` | The calls that count as opening or using a connection |
| `src/test/resources/example/` | An example repo with known answers; `ExampleRepoTest` checks each one |
| `tools/print-classpath.gradle` | Writes a Gradle build's classpath for `--classpath` |
| [`docs/decisions/`](docs/decisions/) | This repo's decisions, in MADR form |
| `build-logic/`, `config/quality/` | The same Gradle conventions and quality rules as under-test |

## Code standards (enforced by `./gradlew check`)
The same as under-test's: at least 80% line coverage; no more than three parameters (use a Lombok `@Builder`); no record
with more than three components; a test with more than one assertion wraps them in `assertAll`; logging through Lombok
`@Slf4j`, never `System.out`; no forbidden JDK APIs; short, simple methods; no copy-pasted code; SpotBugs with
FindSecBugs clean; final locals, braces always, no star imports; lines of 120 characters or fewer. It is built with
Java 25 and runs on Java 17 (`--release 17`).

## Changing what it finds
Every behaviour has a case in the example repo. A fault found on a real repo gets a small case of its own there before
it is fixed, so it stays fixed (under-test's decision on checking tools against real public repos).

## Before you finish
Run `./gradlew check`. It must pass.
